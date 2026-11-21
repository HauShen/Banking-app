package com.Banking_app.notification;

import com.Banking_app.config.kafka.KafkaTopics;
import com.Banking_app.support.AbstractKafkaIntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.header.Header;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * End to end with the real broker: HTTP request → outbox → relay → topic → notification consumer → REST API.
 * Hand-made messages (duplicates, a broken one) are sent straight to the topic with KafkaTemplate.
 */
class NotificationKafkaTest extends AbstractKafkaIntegrationTest {

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    /** The user's notifications whose reference is {@code reference} (up to 100 most recent). */
    private List<JsonNode> notificationsFor(Customer customer, String reference) {
        ResponseEntity<String> response = get("/api/notifications?size=100", customer.token());
        assertStatus(response, 200);
        List<JsonNode> found = new ArrayList<>();
        json(response).get("content").forEach(n -> {
            if (reference.equals(n.get("reference").asText())) {
                found.add(n);
            }
        });
        return found;
    }

    private List<JsonNode> awaitNotification(Customer customer, String reference) {
        await().atMost(Duration.ofSeconds(30)).untilAsserted(() ->
                assertThat(notificationsFor(customer, reference)).isNotEmpty());
        return notificationsFor(customer, reference);
    }

    /** A CASH_DEPOSITED message in the relay's format, for the owner of {@code customer}'s account. */
    private String depositMessage(UUID eventId, String reference, Customer customer) {
        String occurredAt = Instant.now().toString();
        return """
                {"eventId":"%s","eventType":"CASH_DEPOSITED","version":1,"occurredAt":"%s","payload":{"eventId":"%s",\
                "occurredAt":"%s","reference":"%s","accountNumber":"%s","ownerId":"%s","amount":10.00}}"""
                .formatted(eventId, occurredAt, eventId, occurredAt, reference, customer.account().number(),
                        userIdOf(customer.token()));
    }

    private void send(String key, String value) throws Exception {
        kafkaTemplate.send(KafkaTopics.TRANSACTIONS, key, value).get(10, TimeUnit.SECONDS);
    }

    @Test
    @DisplayName("a transfer through the API → the sender gets 'sent', the recipient 'received', within seconds")
    void transferNotifiesBoth() {
        Customer alice = newCustomer("kntf_alice");
        Customer bob = newCustomer("kntf_bob");
        topUp(alice.token(), alice.account(), "100.00");

        ResponseEntity<String> response = transfer(alice.token(), alice.account(), bob.account(), "30.00",
                UUID.randomUUID().toString());
        assertStatus(response, 201);
        String reference = text(response, "transactionReference");

        List<JsonNode> bobs = awaitNotification(bob, reference);
        assertThat(bobs).singleElement().satisfies(n -> {
            assertThat(n.get("type").asText()).isEqualTo("MONEY_RECEIVED");
            assertThat(n.get("message").asText()).startsWith("You received RM 30.00 from account ****");
            assertThat(n.get("read").asBoolean()).isFalse();
        });
        assertThat(awaitNotification(alice, reference)).singleElement()
                .satisfies(n -> assertThat(n.get("type").asText()).isEqualTo("MONEY_SENT"));
    }

    @Test
    @DisplayName("the same message delivered twice → one notification (the consumer is idempotent)")
    void duplicateDelivery() throws Exception {
        Customer carol = newCustomer("kntf_carol");
        String key = carol.account().number();
        UUID eventId = UUID.randomUUID();
        String duplicated = "CDM-DUP-" + eventId.toString().substring(0, 8);
        String marker = "CDM-MARK-" + eventId.toString().substring(0, 8);

        send(key, depositMessage(eventId, duplicated, carol));
        send(key, depositMessage(eventId, duplicated, carol));
        send(key, depositMessage(UUID.randomUUID(), marker, carol));   // same key → same partition → read after both

        awaitNotification(carol, marker);   // so both copies have been processed by now
        assertThat(notificationsFor(carol, duplicated)).hasSize(1);
    }

    @Test
    @DisplayName("a broken message → the dead-letter topic with the reason; the consumer goes on with the next message")
    void poisonMessageGoesToDeadLetterTopic() throws Exception {
        Customer dave = newCustomer("kntf_dave");
        String key = dave.account().number();
        String afterwards = "CDM-AFTER-" + UUID.randomUUID().toString().substring(0, 8);

        send(key, "this is not json");
        send(key, depositMessage(UUID.randomUUID(), afterwards, dave));   // same partition, after the broken one

        awaitNotification(dave, afterwards);   // the consumer was not stuck on the broken message

        try (KafkaConsumer<String, String> dltReader = newConsumer()) {
            List<TopicPartition> partitions = dltReader.partitionsFor(KafkaTopics.TRANSACTIONS_DLT).stream()
                    .map(info -> new TopicPartition(info.topic(), info.partition()))
                    .toList();
            dltReader.assign(partitions);
            dltReader.seekToBeginning(partitions);
            List<ConsumerRecord<String, String>> dead = new ArrayList<>();

            await().atMost(Duration.ofSeconds(30)).pollInSameThread().untilAsserted(() -> {
                dltReader.poll(Duration.ofMillis(500)).forEach(dead::add);
                assertThat(dead).anyMatch(r -> key.equals(r.key()));
            });

            ConsumerRecord<String, String> record = dead.stream().filter(r -> key.equals(r.key())).findFirst()
                    .orElseThrow();
            assertThat(record.value()).isEqualTo("this is not json");
            assertThat(header(record, "kafka_dlt-original-topic")).isEqualTo(KafkaTopics.TRANSACTIONS);
            assertThat(header(record, "kafka_dlt-exception-fqcn") + " " + header(record, "kafka_dlt-exception-cause-fqcn"))
                    .contains("UnprocessableMessageException");
        }
    }

    private static String header(ConsumerRecord<String, String> record, String name) {
        Header header = record.headers().lastHeader(name);
        return header == null ? null : new String(header.value(), StandardCharsets.UTF_8);
    }
}
