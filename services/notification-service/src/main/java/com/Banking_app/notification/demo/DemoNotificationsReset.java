package com.Banking_app.notification.demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * The notification-service's part of the live demo's nightly reset (main app's plan, step 7): empties this service's
 * own database, so yesterday's visitor notifications don't show up for the demo users the main app seeds again.
 *
 * <p>Runs five minutes <em>before</em> the main app's reset (03:55 vs 04:00), so the notifications for the main
 * app's new warm-up transactions arrive in an already clean database and are kept.
 *
 * <p>Empties every table except Flyway's history, read from the database so a later table is never forgotten.
 * {@code processed_events} goes too: the consumer's offsets on the Kafka broker, not this table, decide what is
 * read next, so no old event is processed again.
 */
public class DemoNotificationsReset {

    private static final Logger log = LoggerFactory.getLogger(DemoNotificationsReset.class);

    static final String FLYWAY_HISTORY = "flyway_schema_history";

    private final JdbcTemplate jdbc;
    private final TransactionTemplate transaction;

    public DemoNotificationsReset(JdbcTemplate jdbc, PlatformTransactionManager transactionManager) {
        this.jdbc = Objects.requireNonNull(jdbc, "jdbc");
        this.transaction = new TransactionTemplate(Objects.requireNonNull(transactionManager, "transactionManager"));
    }

    /** Every night at {@code app.demo.reset-cron} (03:55 by default; "-" switches it off). Failures are logged. */
    @Scheduled(cron = "${app.demo.reset-cron:0 55 3 * * *}", zone = "${bank.time-zone:Asia/Kuala_Lumpur}")
    public void nightlyReset() {
        try {
            List<String> tables = reset();
            log.info("Demo reset: emptied {}", tables);
        } catch (RuntimeException e) {
            log.error("Nightly demo reset of the notifications failed; yesterday's notifications are kept", e);
        }
    }

    /** @return the tables that were emptied */
    public List<String> reset() {
        List<String> tables = tablesToEmpty();
        if (tables.isEmpty()) {
            return tables;
        }
        transaction.executeWithoutResult(status -> jdbc.execute("TRUNCATE TABLE "
                + tables.stream().map(t -> '"' + t + '"').collect(Collectors.joining(", "))
                + " RESTART IDENTITY CASCADE"));
        return tables;
    }

    /** All tables of the service's schema except Flyway's history. */
    List<String> tablesToEmpty() {
        return jdbc.queryForList("""
                SELECT table_name FROM information_schema.tables
                WHERE table_schema = current_schema() AND table_type = 'BASE TABLE' AND table_name <> ?
                ORDER BY table_name""", String.class, FLYWAY_HISTORY);
    }
}
