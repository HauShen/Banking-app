package com.Banking_app.alert.adapter.out.persistence;

import com.Banking_app.alert.application.port.out.TransferLogPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

/** alert_transfer_log (V14) with plain SQL. */
@Component
public class TransferLogAdapter implements TransferLogPort {

    private final JdbcTemplate jdbc;

    public TransferLogAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void record(UUID eventId, String accountNumber, Instant occurredAt) {
        jdbc.update("""
                INSERT INTO alert_transfer_log (event_id, account_number, occurred_at) VALUES (?, ?, ?)
                ON CONFLICT (event_id) DO NOTHING""", eventId, accountNumber, utc(occurredAt));
    }

    @Override
    public long countInWindow(String accountNumber, Instant after, Instant upTo) {
        Long count = jdbc.queryForObject("""
                SELECT COUNT(*) FROM alert_transfer_log
                WHERE account_number = ? AND occurred_at > ? AND occurred_at <= ?""",
                Long.class, accountNumber, utc(after), utc(upTo));
        return count == null ? 0 : count;
    }

    @Override
    public void forgetUpTo(String accountNumber, Instant cutoff) {
        jdbc.update("DELETE FROM alert_transfer_log WHERE account_number = ? AND occurred_at <= ?",
                accountNumber, utc(cutoff));
    }

    private static OffsetDateTime utc(Instant instant) {
        return instant.atOffset(ZoneOffset.UTC);
    }
}
