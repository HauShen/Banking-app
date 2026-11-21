package com.Banking_app.outbox;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

/**
 * Plain SQL on {@code outbox_events}. The table is written by the modules (the transaction module's
 * OutboxPersistenceAdapter, in the money movement's transaction); this class only reads and stamps rows for the relay.
 */
@Component
public class JdbcOutboxStore implements OutboxStore {

    /**
     * Any fixed number, the same in every app instance. PostgreSQL's advisory locks are named by numbers: holding this
     * one means "I am the relay right now".
     */
    static final long RELAY_LOCK_ID = 7_301_001L;

    private static final RowMapper<OutboxMessage> MESSAGE = (rs, rowNum) -> new OutboxMessage(
            rs.getLong("id"),
            rs.getObject("event_id", UUID.class),
            rs.getString("topic"),
            rs.getString("message_key"),
            rs.getString("event_type"),
            rs.getInt("event_version"),
            rs.getString("payload"),
            rs.getObject("occurred_at", OffsetDateTime.class).toInstant());

    private final JdbcTemplate jdbc;

    public JdbcOutboxStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * pg_try_advisory_xact_lock: doesn't wait, and is released at the end of the transaction. Only one relay sends at a
     * time, so events leave in id order. (FOR UPDATE SKIP LOCKED would let a second instance send the next rows at the
     * same time, and one account's events could overtake each other.)
     */
    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public boolean tryLockRelay() {
        return Boolean.TRUE.equals(jdbc.queryForObject("SELECT pg_try_advisory_xact_lock(?)", Boolean.class,
                RELAY_LOCK_ID));
    }

    /** payload::text keeps the JSON exactly as stored, numbers included (30.00 stays 30.00). */
    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public List<OutboxMessage> nextBatch(int limit) {
        return jdbc.query("""
                SELECT id, event_id, topic, message_key, event_type, event_version, payload::text AS payload, occurred_at
                FROM outbox_events
                WHERE published_at IS NULL
                ORDER BY id
                LIMIT ?
                FOR UPDATE""", MESSAGE, limit);
    }

    @Override
    public void markPublished(long id, Instant publishedAt) {
        jdbc.update("UPDATE outbox_events SET published_at = ? WHERE id = ?", utc(publishedAt), id);
    }

    @Override
    public void recordFailure(long id) {
        jdbc.update("UPDATE outbox_events SET attempts = attempts + 1 WHERE id = ?", id);
    }

    @Override
    public OutboxBacklog backlog() {
        return jdbc.queryForObject(
                "SELECT COUNT(*) AS waiting, MIN(occurred_at) AS oldest FROM outbox_events WHERE published_at IS NULL",
                (rs, rowNum) -> {
                    OffsetDateTime oldest = rs.getObject("oldest", OffsetDateTime.class);
                    return new OutboxBacklog(rs.getLong("waiting"), oldest == null ? null : oldest.toInstant());
                });
    }

    @Override
    public int deletePublishedBefore(Instant cutoff) {
        return jdbc.update("DELETE FROM outbox_events WHERE published_at < ?", utc(cutoff));
    }

    private static OffsetDateTime utc(Instant instant) {
        return instant.atOffset(ZoneOffset.UTC);
    }
}
