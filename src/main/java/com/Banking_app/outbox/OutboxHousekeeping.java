package com.Banking_app.outbox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

/**
 * Two background jobs around the relay: warn when events have been waiting too long (e.g. Kafka is down), and delete
 * published events after the retention period so the table doesn't grow forever.
 */
@Component
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true")
public class OutboxHousekeeping {

    private static final Logger log = LoggerFactory.getLogger(OutboxHousekeeping.class);

    private final OutboxStore store;
    private final Clock clock;
    private final Duration backlogWarningAfter;
    private final Duration retention;

    public OutboxHousekeeping(OutboxStore store, Clock clock, OutboxProperties properties) {
        this.store = store;
        this.clock = clock;
        this.backlogWarningAfter = properties.backlogWarningAfter();
        this.retention = properties.retention();
    }

    @Scheduled(fixedDelayString = "${app.outbox.backlog-check-interval:PT1M}",
            initialDelayString = "${app.outbox.backlog-check-interval:PT1M}")
    public void warnAboutBacklog() {
        OutboxBacklog backlog = store.backlog();
        if (backlog.waiting() > 0
                && backlog.oldestOccurredAt().isBefore(clock.instant().minus(backlogWarningAfter))) {
            log.warn("Outbox: {} event(s) waiting to be sent to Kafka, the oldest since {}. Is the broker reachable?",
                    backlog.waiting(), backlog.oldestOccurredAt());
        }
    }

    @Scheduled(fixedDelayString = "${app.outbox.cleanup-interval:PT1H}", initialDelayString = "PT1M")
    public void deleteOldPublished() {
        Instant cutoff = clock.instant().minus(retention);
        int deleted = store.deletePublishedBefore(cutoff);
        if (deleted > 0) {
            log.info("Outbox: deleted {} event(s) published before {}", deleted, cutoff);
        }
    }
}
