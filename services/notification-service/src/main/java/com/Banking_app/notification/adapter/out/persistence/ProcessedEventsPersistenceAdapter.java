package com.Banking_app.notification.adapter.out.persistence;

import com.Banking_app.notification.application.port.out.ProcessedEventsPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

/**
 * processed_events with one statement: INSERT … ON CONFLICT DO NOTHING inserts 1 row the first time and 0 rows for a
 * redelivery. If two deliveries of the same event run at the same moment, the second INSERT waits for the first
 * transaction and then inserts nothing — no "check, then insert" race.
 */
@Component
public class ProcessedEventsPersistenceAdapter implements ProcessedEventsPort {

    private final JdbcTemplate jdbc;

    public ProcessedEventsPersistenceAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** MANDATORY: the mark must commit (or roll back) together with the work it stands for. */
    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public boolean markProcessed(String consumer, UUID eventId, Instant at) {
        int inserted = jdbc.update("""
                INSERT INTO processed_events (consumer, event_id, processed_at) VALUES (?, ?, ?)
                ON CONFLICT (consumer, event_id) DO NOTHING""", consumer, eventId, at.atOffset(ZoneOffset.UTC));
        return inserted == 1;
    }
}
