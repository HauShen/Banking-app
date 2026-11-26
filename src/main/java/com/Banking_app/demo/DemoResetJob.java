package com.Banking_app.demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Puts the live demo back to its starting state every night (plan step 7): whatever visitors did during the day
 * (transfers, locked accounts, new customers, deleted users) is gone, and the demo is seeded again for today.
 *
 * <ol>
 *   <li>One database transaction: empty <em>every</em> table except Flyway's history
 *       ({@code TRUNCATE … RESTART IDENTITY CASCADE}), then write the history. PostgreSQL's TRUNCATE is
 *       transactional, so if writing fails, everything rolls back and yesterday's demo stays, rather than an empty
 *       one. The tables are read from the database, not listed by hand, so a table added by a later migration is
 *       never forgotten. The outbox is emptied with the rest, so no event about a deleted transaction is sent
 *       afterwards.</li>
 *   <li>After that transaction has committed: the warm-up (step 5b), through the real use cases and Kafka.</li>
 * </ol>
 * The notification-service empties its own database five minutes earlier (its own reset job), so the warm-up's new
 * notifications arrive in a clean database.
 *
 * <p>While the transaction runs, TRUNCATE locks the tables, so requests wait about a second. At 04:00 that's fine.
 * Logged-in visitors lose their refresh tokens and log in again.
 */
public class DemoResetJob {

    private static final Logger log = LoggerFactory.getLogger(DemoResetJob.class);

    /** Flyway's own record of the applied migrations: emptying it would make Flyway run them all again. */
    static final String FLYWAY_HISTORY = "flyway_schema_history";

    private final JdbcTemplate jdbc;
    private final TransactionTemplate transaction;
    private final DemoDataSeeder seeder;

    public DemoResetJob(JdbcTemplate jdbc, PlatformTransactionManager transactionManager, DemoDataSeeder seeder) {
        this.jdbc = Objects.requireNonNull(jdbc, "jdbc");
        this.transaction = new TransactionTemplate(Objects.requireNonNull(transactionManager, "transactionManager"));
        this.seeder = Objects.requireNonNull(seeder, "seeder");
    }

    /**
     * Every night, at {@code app.demo.reset-cron} in the bank's time zone (04:00 by default; "-" switches it off).
     * A failure is logged and yesterday's demo data stays; the scheduler tries again the next night.
     */
    @Scheduled(cron = "${app.demo.reset-cron:0 0 4 * * *}", zone = "${bank.time-zone:Asia/Kuala_Lumpur}")
    public void nightlyReset() {
        try {
            reset();
        } catch (RuntimeException e) {
            log.error("Nightly demo reset failed; yesterday's demo data is kept", e);
        }
    }

    /**
     * Empties the database and seeds the demo again.
     *
     * @return what the history writer wrote
     * @throws RuntimeException if seeding failed; then nothing was emptied
     */
    public DemoWriteResult reset() {
        Instant started = Instant.now();
        List<String> tables = tablesToEmpty();

        DemoWriteResult result = transaction.execute(status -> {
            jdbc.execute("TRUNCATE TABLE " + tables.stream().map(t -> '"' + t + '"').collect(Collectors.joining(", "))
                    + " RESTART IDENTITY CASCADE");
            return seeder.seedHistoryIfEmpty().orElseThrow(() ->
                    new IllegalStateException("The demo wasn't seeded after emptying the tables"));
        });
        seeder.warmUp();

        log.info("Demo reset done in {} ms: emptied {} tables, wrote {} transactions",
                Duration.between(started, Instant.now()).toMillis(), tables.size(), result.transactions());
        return result;
    }

    /** All tables of the app's schema except Flyway's history. */
    List<String> tablesToEmpty() {
        return jdbc.queryForList("""
                SELECT table_name FROM information_schema.tables
                WHERE table_schema = current_schema() AND table_type = 'BASE TABLE' AND table_name <> ?
                ORDER BY table_name""", String.class, FLYWAY_HISTORY);
    }
}
