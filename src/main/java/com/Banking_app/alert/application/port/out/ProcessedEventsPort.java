package com.Banking_app.alert.application.port.out;

import java.time.Instant;
import java.util.UUID;

/**
 * Remembers which events this module has handled (idempotent consumer). Must run in the same database transaction
 * as the work the event causes. The alert module's own port: modules don't share ports, only the table.
 */
public interface ProcessedEventsPort {

    /** True = first time, go ahead; false = handled before (a redelivery), do nothing. */
    boolean markProcessed(String consumer, UUID eventId, Instant at);
}
