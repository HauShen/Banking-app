package com.Banking_app.outbox;

import java.time.Instant;
import java.util.List;

/** The relay's access to the {@code outbox_events} table. */
public interface OutboxStore {

    /**
     * Tries to become the only relay for the current database transaction. False = another app instance is sending
     * right now; this one skips the round. Released automatically at commit or rollback.
     */
    boolean tryLockRelay();

    /** The oldest unpublished events, in sending order, at most {@code limit}. Needs a transaction. */
    List<OutboxMessage> nextBatch(int limit);

    void markPublished(long id, Instant publishedAt);

    /** Counts a failed send; the event stays unpublished and is tried again next round. */
    void recordFailure(long id);

    OutboxBacklog backlog();

    /** Deletes events published before {@code cutoff}; returns how many. Unpublished events are never deleted. */
    int deletePublishedBefore(Instant cutoff);
}
