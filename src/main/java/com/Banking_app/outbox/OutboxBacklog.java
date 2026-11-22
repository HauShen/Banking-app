package com.Banking_app.outbox;

import java.time.Instant;

/**
 * What is waiting in the outbox.
 *
 * @param oldestOccurredAt when the oldest waiting event happened; null when nothing is waiting
 */
public record OutboxBacklog(long waiting, Instant oldestOccurredAt) {
}
