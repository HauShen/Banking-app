package com.Banking_app.notification.application.port.out;

import java.time.Instant;
import java.util.UUID;

/**
 * Remembers which events a consumer has already handled: the idempotent-consumer pattern.
 *
 * <p>Must be called in the same database transaction as the work the event causes. Then "handled" and "work done"
 * are committed together: if the work fails, the mark rolls back too, and the redelivered event is handled again.
 */
public interface ProcessedEventsPort {

    /**
     * Marks the event as handled by {@code consumer}. True = first time, go ahead; false = it was handled before
     * (a redelivery), do nothing.
     */
    boolean markProcessed(String consumer, UUID eventId, Instant at);
}
