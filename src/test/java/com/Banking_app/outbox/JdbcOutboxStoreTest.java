package com.Banking_app.outbox;

import com.Banking_app.support.AbstractIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The relay's SQL against the real schema (V12). Rows are inserted by hand here.
 *
 * <p>Other test classes leave unpublished events behind (every transfer writes one), so each test first marks every
 * waiting row as published: the outbox is then "empty" and the test sees only its own rows. Its own rows are
 * deleted afterwards, so a later Kafka test class's relay never sends these incomplete events.
 */
class JdbcOutboxStoreTest extends AbstractIntegrationTest {

    private static final Instant T0 = Instant.parse("2026-11-21T08:00:00.123456Z");

    @Autowired
    private JdbcOutboxStore store;

    @Autowired
    private JdbcTemplate jdbc;

    private TransactionTemplate tx;

    @Autowired
    void transactionTemplate(PlatformTransactionManager transactionManager) {
        tx = new TransactionTemplate(transactionManager);
    }

    private final List<Long> inserted = new ArrayList<>();

    @BeforeEach
    void emptyTheOutbox() {
        jdbc.update("UPDATE outbox_events SET published_at = now() WHERE published_at IS NULL");
    }

    @AfterEach
    void deleteOwnRows() {
        inserted.forEach(id -> jdbc.update("DELETE FROM outbox_events WHERE id = ?", id));
    }

    /** Inserts an unpublished event and returns its id. */
    private long insert(String key, Instant occurredAt) {
        Long id = jdbc.queryForObject("""
                INSERT INTO outbox_events (event_id, topic, message_key, event_type, event_version, payload, occurred_at)
                VALUES (?, 'banking.transactions.v1', ?, 'CASH_DEPOSITED', 1, CAST(? AS jsonb), ?)
                RETURNING id""", Long.class,
                UUID.randomUUID(), key, "{\"reference\": \"CDM-1\", \"amount\": 30.00}", occurredAt.atOffset(ZoneOffset.UTC));
        inserted.add(id);
        return id;
    }

    private List<OutboxMessage> nextBatch(int limit) {
        return tx.execute(status -> store.nextBatch(limit));
    }

    @Test
    @DisplayName("nextBatch: unpublished rows only, oldest id first, at most the limit; the payload text keeps 30.00")
    void nextBatchOrderAndLimit() {
        long first = insert("1000000001", T0);
        long second = insert("1000000002", T0.plusSeconds(1));
        insert("1000000003", T0.plusSeconds(2));

        List<OutboxMessage> batch = nextBatch(2);

        assertThat(batch).extracting(OutboxMessage::id).containsExactly(first, second);
        OutboxMessage message = batch.get(0);
        assertThat(message.topic()).isEqualTo("banking.transactions.v1");
        assertThat(message.messageKey()).isEqualTo("1000000001");
        assertThat(message.eventType()).isEqualTo("CASH_DEPOSITED");
        assertThat(message.eventVersion()).isEqualTo(1);
        assertThat(message.occurredAt()).isEqualTo(T0);
        assertThat(message.payload()).contains("30.00");
    }

    @Test
    @DisplayName("markPublished takes a row out of the next batch; recordFailure counts attempts and keeps it waiting")
    void publishAndFailure() {
        long sent = insert("1000000001", T0);
        long failing = insert("1000000002", T0);

        store.markPublished(sent, T0.plusSeconds(5));
        store.recordFailure(failing);
        store.recordFailure(failing);

        assertThat(nextBatch(10)).extracting(OutboxMessage::id).containsExactly(failing);
        assertThat(jdbc.queryForObject("SELECT attempts FROM outbox_events WHERE id = ?", Integer.class, failing))
                .isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT published_at IS NOT NULL FROM outbox_events WHERE id = ?",
                Boolean.class, sent)).isTrue();
    }

    @Test
    @DisplayName("backlog: how many are waiting and since when; nothing waiting → 0 and no time")
    void backlog() {
        assertThat(store.backlog()).isEqualTo(new OutboxBacklog(0, null));

        insert("1000000001", T0.plusSeconds(30));
        insert("1000000002", T0);

        assertThat(store.backlog()).isEqualTo(new OutboxBacklog(2, T0));
    }

    @Test
    @DisplayName("deletePublishedBefore removes only rows published before the cutoff, never unpublished ones")
    void cleanup() {
        Instant now = Instant.now().truncatedTo(ChronoUnit.MICROS);
        long old = insert("1000000001", T0);
        long recent = insert("1000000002", T0);
        long waiting = insert("1000000003", T0.minus(30, ChronoUnit.DAYS));
        store.markPublished(old, now.minus(8, ChronoUnit.DAYS));
        store.markPublished(recent, now.minus(1, ChronoUnit.DAYS));

        int deleted = store.deletePublishedBefore(now.minus(7, ChronoUnit.DAYS));

        assertThat(deleted).isGreaterThanOrEqualTo(1);   // other classes' rows were published "now", so not these
        assertThat(jdbc.queryForList("SELECT id FROM outbox_events WHERE id IN (?, ?, ?)", Long.class,
                old, recent, waiting)).containsExactlyInAnyOrder(recent, waiting);
    }

    @Test
    @DisplayName("the relay lock: while one transaction holds it, a second one (another app instance) can't get it")
    void relayLockIsExclusive() throws Exception {
        CountDownLatch locked = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(1);

        CompletableFuture<Boolean> firstInstance = CompletableFuture.supplyAsync(() -> tx.execute(status -> {
            boolean got = store.tryLockRelay();
            locked.countDown();
            try {
                done.await(10, TimeUnit.SECONDS);   // hold the lock until the second instance has tried
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return got;
        }));
        assertThat(locked.await(10, TimeUnit.SECONDS)).isTrue();

        Boolean secondInstance = tx.execute(status -> store.tryLockRelay());
        done.countDown();

        assertThat(firstInstance.get(10, TimeUnit.SECONDS)).isTrue();
        assertThat(secondInstance).isFalse();
        // released at the end of the first transaction
        Boolean afterwards = tx.execute(status -> store.tryLockRelay());
        assertThat(afterwards).isTrue();
    }

    @Test
    @DisplayName("tryLockRelay and nextBatch refuse to run outside a transaction (the lock and FOR UPDATE would end at once)")
    void needTransaction() {
        assertThatThrownBy(() -> store.tryLockRelay()).isInstanceOf(IllegalTransactionStateException.class);
        assertThatThrownBy(() -> store.nextBatch(1)).isInstanceOf(IllegalTransactionStateException.class);
    }
}
