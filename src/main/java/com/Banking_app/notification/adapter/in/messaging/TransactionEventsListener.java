package com.Banking_app.notification.adapter.in.messaging;

import com.Banking_app.config.kafka.KafkaTopics;
import com.Banking_app.config.kafka.UnprocessableMessageException;
import com.Banking_app.notification.application.port.in.command.CashDepositedNotice;
import com.Banking_app.notification.application.port.in.command.RemittanceReceivedNotice;
import com.Banking_app.notification.application.port.in.command.TransferCompletedNotice;
import com.Banking_app.notification.application.port.in.usecase.RecordNotificationsUseCase;
import com.Banking_app.notification.domain.models.ForeignAmount;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.UUID;

/**
 * Inbound messaging adapter: reads the transaction events from Kafka and calls the notification use case.
 *
 * <p>It knows only the message format (the envelope {eventId, eventType, version, occurredAt, payload} and the
 * payload fields), never the transaction module's classes: the topic is the contract between the two modules.
 *
 * <ul>
 *   <li>Unknown event types are skipped: a producer may add new events before this consumer cares about them.</li>
 *   <li>A message that can't be read (not JSON, missing field, unknown version) →
 *       {@link UnprocessableMessageException} → straight to the dead-letter topic.</li>
 *   <li>Anything else that fails (e.g. the database) is retried by the error handler, then dead-lettered.</li>
 * </ul>
 * Consumer group {@value #GROUP_ID}: its offsets on the broker say how far this consumer has read. Other consumers
 * (e.g. alerts) use their own group and read the same messages independently.
 */
@Component
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true")
public class TransactionEventsListener {

    public static final String GROUP_ID = "banking-notifications";
    static final int SUPPORTED_VERSION = 1;

    private static final Logger log = LoggerFactory.getLogger(TransactionEventsListener.class);

    /** Numbers as BigDecimal, never double: amounts must keep every digit. */
    private static final ObjectMapper JSON = JsonMapper.builder()
            .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
            .build();

    private final RecordNotificationsUseCase recordNotificationsUseCase;

    public TransactionEventsListener(RecordNotificationsUseCase recordNotificationsUseCase) {
        this.recordNotificationsUseCase = recordNotificationsUseCase;
    }

    @KafkaListener(topics = KafkaTopics.TRANSACTIONS, groupId = GROUP_ID)
    public void onMessage(ConsumerRecord<String, String> record) {
        JsonNode envelope = parse(record.value());
        String eventType = text(envelope, "eventType");
        int version = required(envelope, "version").asInt();
        if (version != SUPPORTED_VERSION) {
            throw new UnprocessableMessageException(eventType + " version " + version + " is not supported");
        }
        UUID eventId = uuid(envelope, "eventId");
        Instant occurredAt = instant(envelope, "occurredAt");
        JsonNode payload = required(envelope, "payload");

        switch (eventType) {
            case "TRANSFER_COMPLETED" -> recordNotificationsUseCase.transferCompleted(new TransferCompletedNotice(
                    eventId, occurredAt, text(payload, "reference"),
                    text(payload, "fromAccountNumber"), text(payload, "fromOwnerId"),
                    text(payload, "toAccountNumber"), text(payload, "toOwnerId"),
                    decimal(payload, "amount"), foreignAmount(payload)));
            case "CASH_DEPOSITED" -> recordNotificationsUseCase.cashDeposited(new CashDepositedNotice(
                    eventId, occurredAt, text(payload, "reference"),
                    text(payload, "accountNumber"), text(payload, "ownerId"), decimal(payload, "amount")));
            case "FOREIGN_REMITTANCE_RECEIVED" -> recordNotificationsUseCase.remittanceReceived(
                    new RemittanceReceivedNotice(eventId, occurredAt, text(payload, "reference"),
                            text(payload, "accountNumber"), text(payload, "ownerId"), decimal(payload, "amount"),
                            foreignAmount(payload)));
            default -> log.debug("Notifications: no notification for event type {}, skipped", eventType);
        }
    }

    // ------------------------------------------------------------------ reading the JSON

    private static JsonNode parse(String value) {
        if (value == null) {
            throw new UnprocessableMessageException("Empty message");
        }
        try {
            return JSON.readTree(value);
        } catch (JsonProcessingException e) {
            throw new UnprocessableMessageException("Message is not valid JSON", e);
        }
    }

    private static JsonNode required(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            throw new UnprocessableMessageException("Missing field '" + field + "'");
        }
        return value;
    }

    private static String text(JsonNode node, String field) {
        return required(node, field).asText();
    }

    private static BigDecimal decimal(JsonNode node, String field) {
        JsonNode value = required(node, field);
        if (!value.isNumber()) {
            throw new UnprocessableMessageException("Field '" + field + "' is not a number");
        }
        return value.decimalValue();
    }

    private static UUID uuid(JsonNode node, String field) {
        try {
            return UUID.fromString(text(node, field));
        } catch (IllegalArgumentException e) {
            throw new UnprocessableMessageException("Field '" + field + "' is not a UUID", e);
        }
    }

    private static Instant instant(JsonNode node, String field) {
        try {
            return Instant.parse(text(node, field));
        } catch (DateTimeParseException e) {
            throw new UnprocessableMessageException("Field '" + field + "' is not a time", e);
        }
    }

    /** {"currency":"USD","amount":20.00,…} or null / absent for a MYR transaction. */
    private static ForeignAmount foreignAmount(JsonNode payload) {
        JsonNode original = payload.get("original");
        if (original == null || original.isNull()) {
            return null;
        }
        return new ForeignAmount(text(original, "currency"), decimal(original, "amount"));
    }
}
