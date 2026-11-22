package com.Banking_app.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.util.RawValue;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/**
 * Sends an outbox message to Kafka and waits for the broker's confirmation (acks=all).
 *
 * <p>The message value is an envelope around the stored event:
 * <pre>
 * {"eventId":"…","eventType":"TRANSFER_COMPLETED","version":1,"occurredAt":"2026-11-21T…Z","payload":{…the event…}}
 * </pre>
 * eventId and eventType are also sent as headers, so a consumer can route or skip a message without parsing it.
 */
@Component
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true")
public class KafkaOutboxSender implements OutboxSender {

    static final String HEADER_EVENT_ID = "eventId";
    static final String HEADER_EVENT_TYPE = "eventType";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final OutboxProperties properties;

    public KafkaOutboxSender(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper,
                             OutboxProperties properties) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    @Override
    public void send(OutboxMessage message) throws Exception {
        ProducerRecord<String, String> record =
                new ProducerRecord<>(message.topic(), message.messageKey(), envelope(message));
        record.headers()
                .add(HEADER_EVENT_ID, message.eventId().toString().getBytes(StandardCharsets.UTF_8))
                .add(HEADER_EVENT_TYPE, message.eventType().getBytes(StandardCharsets.UTF_8));

        // Blocks until the broker confirms, so the relay only marks a row published when it really is.
        kafkaTemplate.send(record).get(properties.sendTimeout().toMillis(), TimeUnit.MILLISECONDS);
    }

    /**
     * The payload goes in as raw JSON text, not parsed and written again: parsing would turn 30.00 into the double
     * 30.0, and money must keep its exact digits.
     */
    String envelope(OutboxMessage message) throws JsonProcessingException {
        ObjectNode envelope = objectMapper.createObjectNode();
        envelope.put("eventId", message.eventId().toString());
        envelope.put("eventType", message.eventType());
        envelope.put("version", message.eventVersion());
        envelope.put("occurredAt", message.occurredAt().toString());
        envelope.putRawValue("payload", new RawValue(message.payload()));
        return objectMapper.writeValueAsString(envelope);
    }
}
