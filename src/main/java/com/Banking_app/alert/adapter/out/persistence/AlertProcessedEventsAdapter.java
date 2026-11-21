package com.Banking_app.alert.adapter.out.persistence;

import com.Banking_app.alert.application.port.out.ProcessedEventsPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

/**
 * processed_events (V13), shared table, own rows (consumer = "alerts"): INSERT … ON CONFLICT DO NOTHING inserts one
 * row the first time and none for a redelivery.
 */
@Component
public class AlertProcessedEventsAdapter implements ProcessedEventsPort {

    private final JdbcTemplate jdbc;

    public AlertProcessedEventsAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public boolean markProcessed(String consumer, UUID eventId, Instant at) {
        return jdbc.update("""
                INSERT INTO processed_events (consumer, event_id, processed_at) VALUES (?, ?, ?)
                ON CONFLICT (consumer, event_id) DO NOTHING""", consumer, eventId, at.atOffset(ZoneOffset.UTC)) == 1;
    }
}
