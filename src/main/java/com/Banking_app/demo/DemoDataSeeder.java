package com.Banking_app.demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;

/**
 * Fills an empty database with the demo at startup (plan step 5). Runs after Flyway, which Spring Boot guarantees
 * for every ApplicationRunner.
 *
 * <p>Seeds only when <em>none</em> of the demo data is there yet: not one of the cast's usernames and not one of
 * its account numbers. Checking only "weiming" isn't enough: a visitor logged in as demoadmin could delete some demo
 * users, and a restart would then try to insert the others again and fail on the unique constraints. If the demo
 * data is only partly there, the seeder leaves it alone and logs a warning; the nightly reset (step 7) puts it back.
 *
 * <p>If writing fails, the exception stops the app from starting. That's on purpose: the writer only fails on a
 * bug, and a demo that starts with broken or half-written data is worse than one that doesn't start.
 *
 * <p>After the history has committed, the {@link DemoWarmUp} makes four real transactions (step 5b). If one of them
 * fails, the error is logged and the app still starts: the history is complete, only the bell and the Alerts page
 * start empty. Not worth a demo that's down.
 */
public class DemoDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

    private final JdbcTemplate jdbc;
    private final DemoStoryGenerator generator;
    private final DemoDataWriter writer;
    private final DemoWarmUp warmUp;
    private final DemoProperties properties;
    private final Clock clock;

    public DemoDataSeeder(JdbcTemplate jdbc, DemoStoryGenerator generator, DemoDataWriter writer,
                          DemoWarmUp warmUp, DemoProperties properties, Clock clock) {
        this.jdbc = Objects.requireNonNull(jdbc, "jdbc");
        this.generator = Objects.requireNonNull(generator, "generator");
        this.writer = Objects.requireNonNull(writer, "writer");
        this.warmUp = Objects.requireNonNull(warmUp, "warmUp");
        this.properties = Objects.requireNonNull(properties, "properties");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Override
    public void run(ApplicationArguments args) {
        seedIfEmpty();
    }

    /**
     * @return what was written, or empty if demo data was already there (fully or partly) and nothing was written
     */
    public Optional<DemoWriteResult> seedIfEmpty() {
        DemoCast cast = DemoCast.standard(properties.historyDays());

        int present = countPresent(cast);
        int total = cast.users().size() + cast.accounts().size();
        if (present == total) {
            log.info("Demo data already present, nothing to seed");
            return Optional.empty();
        }
        if (present > 0) {
            log.warn("Demo data is only partly present ({} of {} demo users and accounts); not seeding. "
                    + "The nightly demo reset restores it.", present, total);
            return Optional.empty();
        }

        Instant started = Instant.now();
        Instant now = clock.instant();
        DemoStory story = generator.generate(cast, now, new Random(properties.randomSeed()));
        DemoWriteResult result = writer.write(cast, story, now);   // committed when this returns
        List<String> warmUpReferences = runWarmUp(cast);

        log.info("Demo data seeded in {} ms: {} users, {} accounts, {} transactions, {} ledger rows "
                        + "({} days of history, seed {}), warm-up {}",
                Duration.between(started, Instant.now()).toMillis(), result.users(), result.accounts(),
                result.transactions(), result.ledgerRows(), properties.historyDays(), properties.randomSeed(),
                warmUpReferences);
        return Optional.of(result);
    }

    private List<String> runWarmUp(DemoCast cast) {
        try {
            return warmUp.run(cast);
        } catch (RuntimeException e) {
            log.error("Demo warm-up failed: the history is seeded, but the bell and the Alerts page start empty", e);
            return List.of();
        }
    }

    /** How many of the cast's usernames and account numbers are already in the database. */
    private int countPresent(DemoCast cast) {
        int present = 0;
        for (DemoUser user : cast.users()) {
            if (exists("SELECT EXISTS (SELECT 1 FROM users WHERE username = ?)", user.username())) {
                present++;
            }
        }
        for (DemoAccount account : cast.accounts()) {
            if (exists("SELECT EXISTS (SELECT 1 FROM accounts WHERE account_number = ?)", account.accountNumber())) {
                present++;
            }
        }
        return present;
    }

    private boolean exists(String sql, String value) {
        return Boolean.TRUE.equals(jdbc.queryForObject(sql, Boolean.class, value));
    }
}
