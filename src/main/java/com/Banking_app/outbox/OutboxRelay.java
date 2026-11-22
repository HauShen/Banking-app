package com.Banking_app.outbox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionOperations;

import java.time.Clock;
import java.util.List;

/**
 * Sends waiting outbox events to Kafka, oldest first, about once a second.
 *
 * <p>Each batch runs in one database transaction: lock the relay, read up to batch-size unpublished rows, send them
 * one by one, stamp each sent row with published_at. The rules that matter:
 * <ul>
 *   <li><b>Stop at the first failure.</b> The failed row gets attempts + 1 and stays unpublished, and so do the rows
 *       after it, so no later event overtakes it. Next round starts again at the same row. While Kafka is down, nothing
 *       moves on; the transfers themselves keep working and their events wait here.</li>
 *   <li><b>At least once, not exactly once.</b> If the app dies after Kafka confirmed a send but before the commit,
 *       the row is still unpublished and is sent again. Consumers recognise the repeat by its eventId.</li>
 *   <li><b>One relay at a time</b> (an advisory lock), so events leave in id order even with several app instances.</li>
 * </ul>
 */
@Component
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true")
public class OutboxRelay {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelay.class);

    private final OutboxStore store;
    private final OutboxSender sender;
    private final TransactionOperations transactions;
    private final Clock clock;
    private final int batchSize;

    public OutboxRelay(OutboxStore store, OutboxSender sender, TransactionOperations transactions, Clock clock,
                       OutboxProperties properties) {
        this.store = store;
        this.sender = sender;
        this.transactions = transactions;
        this.clock = clock;
        this.batchSize = properties.batchSize();
    }

    /**
     * One round: batch after batch until the outbox is empty, a send fails, or another instance holds the relay.
     * fixedDelay = the next round starts this long after the previous one ended, so rounds never overlap.
     */
    @Scheduled(fixedDelayString = "${app.outbox.relay-interval:PT1S}")
    public void relayPending() {
        Batch batch;
        do {
            batch = transactions.execute(status -> relayOneBatch());
        } while (batch != null && batch.full() && !batch.failed());
    }

    private Batch relayOneBatch() {
        if (!store.tryLockRelay()) {
            return Batch.SKIPPED;   // another instance is relaying right now
        }
        List<OutboxMessage> messages = store.nextBatch(batchSize);
        for (OutboxMessage message : messages) {
            try {
                sender.send(message);
            } catch (Exception e) {
                if (e instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
                store.recordFailure(message.id());
                // No stack trace: while Kafka is down this repeats every round. OutboxHousekeeping warns about the
                // growing backlog once a minute.
                log.warn("Outbox: sending {} {} (row {}) failed, will retry: {}", message.eventType(),
                        message.eventId(), message.id(), e.toString());
                return new Batch(true, false);
            }
            store.markPublished(message.id(), clock.instant());
        }
        return new Batch(false, messages.size() == batchSize);
    }

    /**
     * @param failed a send failed: stop for this round
     * @param full   the batch was full, so more may be waiting: go on with the next batch
     */
    private record Batch(boolean failed, boolean full) {
        static final Batch SKIPPED = new Batch(false, false);
    }
}
