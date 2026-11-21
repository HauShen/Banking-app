package com.Banking_app.outbox;

import java.time.Instant;
import java.util.UUID;

/**
 * One row of {@code outbox_events} that still has to be sent.
 *
 * @param id         the row id, also the sending order
 * @param messageKey the Kafka key (an account number): same key → same partition → read in order
 * @param payload    the event as JSON text, exactly as stored
 */
public record OutboxMessage(long id,
                            UUID eventId,
                            String topic,
                            String messageKey,
                            String eventType,
                            int eventVersion,
                            String payload,
                            Instant occurredAt) {
}
