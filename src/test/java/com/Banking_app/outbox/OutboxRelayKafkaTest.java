package com.Banking_app.outbox;

import com.Banking_app.config.kafka.KafkaTopics;
import com.Banking_app.support.AbstractKafkaIntegrationTest;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.cfg.JsonNodeFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.header.Header;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * End to end: HTTP request → outbox row (same transaction) → relay → Kafka topic. Reads the topic with a plain
 * consumer of its own; the app has no consumers yet (Phase 4).
 */
class OutboxRelayKafkaTest extends AbstractKafkaIntegrationTest {

    /**
     * Reads numbers exactly as written: as BigDecimal (not double), and without the JSON tree's default of stripping
     * trailing zeros (which would turn 30.00 into 3E+1).
     */
    private static final ObjectMapper EXACT = JsonMapper.builder()
            .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
            .configure(JsonNodeFeature.STRIP_TRAILING_BIGDECIMAL_ZEROES, false)
            .build();

    @Autowired
    private JdbcTemplate jdbc;

    private KafkaConsumer<String, String> consumer;
    private final List<ConsumerRecord<String, String>> received = new ArrayList<>();

    @BeforeEach
    void readTheTopicFromTheStart() {
        consumer = newConsumer();
        List<TopicPartition> partitions = consumer.partitionsFor(KafkaTopics.TRANSACTIONS).stream()
                .map(info -> new TopicPartition(info.topic(), info.partition()))
                .toList();
        consumer.assign(partitions);
        consumer.seekToBeginning(partitions);
    }

    @AfterEach
    void closeConsumer() {
        consumer.close();
    }

    /** Polls until the topic holds {@code count} messages with this key; returns them in the order they were read. */
    private List<ConsumerRecord<String, String>> awaitMessagesWithKey(String key, int count) {
        await().atMost(Duration.ofSeconds(30)).pollInSameThread().untilAsserted(() -> {
            consumer.poll(Duration.ofMillis(500)).forEach(received::add);
            assertThat(received.stream().filter(r -> key.equals(r.key()))).hasSizeGreaterThanOrEqualTo(count);
        });
        return received.stream().filter(r -> key.equals(r.key())).toList();
    }

    private static String header(ConsumerRecord<String, String> record, String name) {
        Header header = record.headers().lastHeader(name);
        return header == null ? null : new String(header.value(), StandardCharsets.UTF_8);
    }

    private String eventIdOf(String reference) {
        return jdbc.queryForObject("SELECT event_id::text FROM outbox_events WHERE payload->>'reference' = ?",
                String.class, reference);
    }

    @Test
    @DisplayName("a transfer arrives on the topic: key = sender's account, eventId/eventType headers, envelope with the exact payload")
    void transferReachesKafka() throws Exception {
        Customer alice = newCustomer("relay_alice");
        Customer bob = newCustomer("relay_bob");
        topUp(alice.token(), alice.account(), "100.00");

        ResponseEntity<String> response = transfer(alice.token(), alice.account(), bob.account(), "30.00",
                UUID.randomUUID().toString());
        assertStatus(response, 201);
        String reference = text(response, "transactionReference");
        String eventId = eventIdOf(reference);

        ConsumerRecord<String, String> record = awaitMessagesWithKey(alice.account().number(), 2).stream()
                .filter(r -> eventId.equals(header(r, "eventId")))
                .findFirst().orElseThrow();   // the other message with this key is the top-up

        assertThat(record.topic()).isEqualTo("banking.transactions.v1");
        assertThat(header(record, "eventType")).isEqualTo("TRANSFER_COMPLETED");
        assertThat(record.value()).contains("\"amount\": 30.00");   // the text on the wire, before any parsing

        JsonNode envelope = EXACT.readTree(record.value());
        assertThat(envelope.get("eventId").asText()).isEqualTo(eventId);
        assertThat(envelope.get("eventType").asText()).isEqualTo("TRANSFER_COMPLETED");
        assertThat(envelope.get("version").asInt()).isEqualTo(1);
        assertThat(envelope.get("occurredAt").asText()).endsWith("Z");
        JsonNode payload = envelope.get("payload");
        assertThat(payload.get("reference").asText()).isEqualTo(reference);
        assertThat(payload.get("toAccountNumber").asText()).isEqualTo(bob.account().number());
        assertThat(payload.get("amount").decimalValue()).isEqualTo(new BigDecimal("30.00"));   // scale kept

        // and the row is stamped as sent
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> assertThat(jdbc.queryForObject(
                "SELECT published_at IS NOT NULL FROM outbox_events WHERE payload->>'reference' = ?",
                Boolean.class, reference)).isTrue());
    }

    @Test
    @DisplayName("one account's events land on one partition, in the order they happened")
    void sameKeySamePartitionInOrder() {
        Customer carol = newCustomer("relay_carol");
        topUp(carol.token(), carol.account(), "10.00");
        topUp(carol.token(), carol.account(), "20.00");
        topUp(carol.token(), carol.account(), "30.00");

        List<String> writtenOrder = jdbc.queryForList(
                "SELECT event_id::text FROM outbox_events WHERE message_key = ? ORDER BY id", String.class,
                carol.account().number());

        List<ConsumerRecord<String, String>> records = awaitMessagesWithKey(carol.account().number(), 3);

        assertThat(records).extracting(ConsumerRecord::partition).containsOnly(records.get(0).partition());
        assertThat(records).extracting(r -> header(r, "eventId")).containsExactlyElementsOf(writtenOrder);
        assertThat(records).extracting(ConsumerRecord::offset).isSorted();
    }
}
