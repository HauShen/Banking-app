package com.Banking_app.outbox;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** The outbox table in memory: rows in id order, with published time and failed attempts. */
class FakeOutboxStore implements OutboxStore {

    final List<OutboxMessage> rows = new ArrayList<>();
    final Map<Long, Instant> publishedAt = new HashMap<>();
    final Map<Long, Integer> attempts = new HashMap<>();
    boolean relayLockFree = true;
    int batchesRead;

    OutboxMessage add(String key) {
        long id = rows.size() + 1;
        OutboxMessage message = new OutboxMessage(id, UUID.randomUUID(), "banking.transactions.v1", key,
                "CASH_DEPOSITED", 1, "{\"reference\":\"CDM-" + id + "\"}", Instant.parse("2026-11-21T00:00:00Z"));
        rows.add(message);
        return message;
    }

    boolean isPublished(OutboxMessage message) {
        return publishedAt.containsKey(message.id());
    }

    @Override
    public boolean tryLockRelay() {
        return relayLockFree;
    }

    @Override
    public List<OutboxMessage> nextBatch(int limit) {
        batchesRead++;
        return rows.stream().filter(m -> !isPublished(m)).limit(limit).toList();
    }

    @Override
    public void markPublished(long id, Instant at) {
        publishedAt.put(id, at);
    }

    @Override
    public void recordFailure(long id) {
        attempts.merge(id, 1, Integer::sum);
    }

    @Override
    public OutboxBacklog backlog() {
        List<OutboxMessage> waiting = rows.stream().filter(m -> !isPublished(m)).toList();
        return new OutboxBacklog(waiting.size(), waiting.isEmpty() ? null : waiting.get(0).occurredAt());
    }

    @Override
    public int deletePublishedBefore(Instant cutoff) {
        return 0;
    }
}
