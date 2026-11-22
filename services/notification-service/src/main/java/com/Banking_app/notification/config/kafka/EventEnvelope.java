package com.Banking_app.notification.config.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.UUID;

/**
 * The envelope the main app's outbox relay puts around every event on Kafka (KafkaOutboxSender there):
 * <pre>
 * {"eventId":"…","eventType":"TRANSFER_COMPLETED","version":1,"occurredAt":"2026-11-21T…Z","payload":{…}}
 * </pre>
 * Consumers read it with {@link #parse(String)} and the payload's fields with the static helpers. Anything that can't
 * be read throws {@link UnprocessableMessageException}, which the error handler sends straight to the dead-letter
 * topic. Numbers are read as BigDecimal, never double, so amounts keep every digit.
 *
 * <p>A copy of the main app's class of the same name, not a shared library: the message format is the only thing the
 * producer and this service have in common, and each side reads it with its own code.
 */
public record EventEnvelope(UUID eventId, String eventType, int version, Instant occurredAt, JsonNode payload) {

    private static final ObjectMapper JSON = JsonMapper.builder()
            .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
            .build();

    public static EventEnvelope parse(String value) {
        if (value == null) {
            throw new UnprocessableMessageException("Empty message");
        }
        JsonNode envelope;
        try {
            envelope = JSON.readTree(value);
        } catch (JsonProcessingException e) {
            throw new UnprocessableMessageException("Message is not valid JSON", e);
        }
        String eventType = text(envelope, "eventType");
        int version = required(envelope, "version").asInt();
        return new EventEnvelope(uuid(envelope, "eventId"), eventType, version, instant(envelope, "occurredAt"),
                required(envelope, "payload"));
    }

    /** A consumer reads only the payload versions it knows; any other → dead-letter topic. */
    public void requireVersion(int supported) {
        if (version != supported) {
            throw new UnprocessableMessageException(eventType + " version " + version + " is not supported");
        }
    }

    // ------------------------------------------------------------------ payload fields

    public static JsonNode required(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            throw new UnprocessableMessageException("Missing field '" + field + "'");
        }
        return value;
    }

    /** The field, or null when it is absent or JSON null (e.g. "original" of a MYR transfer). */
    public static JsonNode optional(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value;
    }

    public static String text(JsonNode node, String field) {
        return required(node, field).asText();
    }

    public static BigDecimal decimal(JsonNode node, String field) {
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
}
