package com.Banking_app.demo;

import com.Banking_app.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The seeder against the real schema. Like DemoDataWriterTest, every test runs inside a transaction that is rolled
 * back, so no demo rows stay in the shared test database. The seeder is built by hand: the demo profile isn't active
 * in the shared test context (step 6 tests the whole app with the profile on).
 */
class DemoDataSeederTest extends AbstractIntegrationTest {

    private static final ZoneId KL = ZoneId.of("Asia/Kuala_Lumpur");
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 26);
    private static final Instant NOW = ZonedDateTime.of(TODAY, LocalTime.NOON, KL).toInstant();

    @Autowired private JdbcTemplate jdbc;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private PasswordEncoder passwordEncoder;

    @Test
    void emptyDatabase_getsTheWholeDemo() {
        inRolledBackTransaction(() -> {
            Optional<DemoWriteResult> result = seeder(90).seedIfEmpty();

            assertThat(result).isPresent();
            assertThat(result.get().users()).isEqualTo(6);
            assertThat(result.get().accounts()).isEqualTo(7);
            assertThat(result.get().transactions()).isGreaterThan(150);
            assertThat(count("SELECT COUNT(*) FROM users WHERE email LIKE '%@banking.demo'")).isEqualTo(6);
            return null;
        });
    }

    @Test
    void secondRun_writesNothing() {
        inRolledBackTransaction(() -> {
            seeder(90).seedIfEmpty();
            long transactions = count("SELECT COUNT(*) FROM transactions");

            Optional<DemoWriteResult> second = seeder(90).seedIfEmpty();

            assertThat(second).isEmpty();
            assertThat(count("SELECT COUNT(*) FROM transactions")).isEqualTo(transactions);
            return null;
        });
    }

    @Test
    void partlyPresentDemo_isLeftAlone() {
        // e.g. a visitor deleted some demo users and the app restarted: inserting the rest again would fail.
        inRolledBackTransaction(() -> {
            jdbc.update("""
                    INSERT INTO users (id, username, role, full_name, email, password, created_at, updated_at)
                    VALUES (?, 'demoadmin', 'ADMIN', 'Demo Admin', 'demoadmin@banking.demo', 'x', now(), now())""",
                    DemoCast.standard(90).user("demoadmin").id());

            Optional<DemoWriteResult> result = seeder(90).seedIfEmpty();

            assertThat(result).isEmpty();
            assertThat(count("SELECT COUNT(*) FROM users WHERE username = 'weiming'")).isZero();
            return null;
        });
    }

    @Test
    void usesTheDemoSettingsAndTheClock() {
        inRolledBackTransaction(() -> {
            seeder(30).seedIfEmpty();

            OffsetDateTime openedAt = jdbc.queryForObject(
                    "SELECT created_at FROM accounts WHERE account_number = ?", OffsetDateTime.class,
                    DemoCast.MAIN_SAVINGS);
            assertThat(openedAt.toInstant()).isEqualTo(
                    TODAY.minusDays(29).atTime(DemoAccount.OPENING_TIME).atZone(KL).toInstant());

            Map<String, Object> weiming = jdbc.queryForMap("SELECT password FROM users WHERE username = 'weiming'");
            assertThat(passwordEncoder.matches("Demo1234!", (String) weiming.get("password"))).isTrue();

            OffsetDateTime newest = jdbc.queryForObject("""
                    SELECT MAX(t.created_at) FROM transactions t JOIN accounts a ON a.id = t.to_account_id
                    WHERE a.account_number IN (?, ?, ?, ?, ?, ?, ?)""", OffsetDateTime.class,
                    DemoCast.MAIN_SAVINGS, DemoCast.MAIN_CURRENT, DemoCast.MEI_LING_SAVINGS, DemoCast.AHMAD_CURRENT,
                    DemoCast.AHMAD_SAVINGS_LOCKED, DemoCast.PRIYA_SAVINGS, DemoCast.JASON_SAVINGS_NEW);
            assertThat(newest.toInstant()).isBeforeOrEqualTo(NOW.minusSeconds(60));
            return null;
        });
    }

    private DemoDataSeeder seeder(int historyDays) {
        DemoProperties properties = new DemoProperties("Demo1234!", "DemoAdmin1!", 42L, historyDays);
        DemoStoryGenerator generator = new DemoStoryGenerator(KL,
                Map.of("USD", new BigDecimal("4.1650"), "SGD", new BigDecimal("3.2100")));
        DemoDataWriter writer = new DemoDataWriter(jdbc, transactionManager, passwordEncoder, properties, KL);
        return new DemoDataSeeder(jdbc, generator, writer, properties, Clock.fixed(NOW, KL));
    }

    private <T> T inRolledBackTransaction(Supplier<T> work) {
        return new TransactionTemplate(transactionManager).execute(status -> {
            status.setRollbackOnly();
            return work.get();
        });
    }

    private long count(String sql) {
        Long n = jdbc.queryForObject(sql, Long.class);
        return n == null ? 0 : n;
    }
}
