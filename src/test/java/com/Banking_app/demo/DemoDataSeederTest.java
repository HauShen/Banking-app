package com.Banking_app.demo;

import com.Banking_app.support.AbstractIntegrationTest;
import com.Banking_app.transaction.application.port.in.usecase.RecordForeignRemittanceUseCase;
import com.Banking_app.transaction.application.port.in.usecase.TransferMoneyUseCase;
import jakarta.persistence.EntityManager;
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
import java.util.List;
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
    @Autowired private TransferMoneyUseCase transferMoneyUseCase;
    @Autowired private RecordForeignRemittanceUseCase recordForeignRemittanceUseCase;
    @Autowired private EntityManager entityManager;

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

            // The history's newest row (keys "demo-…"); the warm-up's rows come later, from the app's real clock.
            OffsetDateTime newest = jdbc.queryForObject("""
                    SELECT MAX(t.created_at) FROM transactions t JOIN accounts a ON a.id = t.to_account_id
                    WHERE t.idempotency_key LIKE 'demo-%' AND a.account_number IN (?, ?, ?, ?, ?, ?, ?)""",
                    OffsetDateTime.class,
                    DemoCast.MAIN_SAVINGS, DemoCast.MAIN_CURRENT, DemoCast.MEI_LING_SAVINGS, DemoCast.AHMAD_CURRENT,
                    DemoCast.AHMAD_SAVINGS_LOCKED, DemoCast.PRIYA_SAVINGS, DemoCast.JASON_SAVINGS_NEW);
            assertThat(newest.toInstant()).isBeforeOrEqualTo(NOW.minusSeconds(60));
            return null;
        });
    }

    @Test
    void seeding_endsWithTheFourWarmUpTransactions_throughTheRealUseCases() {
        // The use cases stamp the app's real clock, which is later than the story's fixed NOW: the warm-up rows
        // are the newest, as they are in the live demo.
        inRolledBackTransaction(() -> {
            long outboxBefore = count("SELECT COUNT(*) FROM outbox_events");

            seeder(90).seedIfEmpty();
            // The use cases save through JPA, which may hold back some writes (the balance update, the outbox row)
            // until a flush. In the live app each use case commits; here everything shares one transaction.
            entityManager.flush();

            List<Map<String, Object>> newest = jdbc.queryForList("""
                    SELECT * FROM (
                        SELECT t.* FROM transactions t JOIN accounts a ON a.id = t.to_account_id
                        WHERE a.user_id IN (SELECT id FROM users WHERE email LIKE '%@banking.demo')
                        ORDER BY t.created_at DESC, t.id DESC LIMIT 4) last_four
                    ORDER BY created_at, id""");
            assertThat(newest).extracting(row -> row.get("description")).containsExactly(
                    "Dinner split", "Grab ride share", "Foreign remittance from Lee Chong", "Car down payment");
            assertThat(newest).extracting(row -> row.get("idempotency_key")).containsExactly(
                    DemoWarmUp.idempotencyKey(1), DemoWarmUp.idempotencyKey(2), DemoWarmUp.idempotencyKey(3),
                    DemoWarmUp.idempotencyKey(4));
            assertThat((BigDecimal) newest.get(3).get("amount")).isEqualByComparingTo("10500.00");

            // Only the warm-up reaches Kafka: one outbox event each, none for the history.
            assertThat(count("SELECT COUNT(*) FROM outbox_events")).isEqualTo(outboxBefore + 4);

            // The real use case kept ahmad.f's balance and ledger in step.
            BigDecimal balance = jdbc.queryForObject("SELECT current_balance FROM accounts WHERE account_number = ?",
                    BigDecimal.class, DemoCast.AHMAD_CURRENT);
            BigDecimal lastBalanceAfter = jdbc.queryForObject("""
                    SELECT l.balance_after FROM ledgers l JOIN accounts a ON a.id = l.account_id
                    WHERE a.account_number = ? ORDER BY l.created_at DESC, l.id DESC LIMIT 1""",
                    BigDecimal.class, DemoCast.AHMAD_CURRENT);
            assertThat(balance).isEqualByComparingTo(lastBalanceAfter);
            assertThat(balance.signum()).isNotNegative();
            return null;
        });
    }

    private DemoDataSeeder seeder(int historyDays) {
        DemoProperties properties = new DemoProperties("Demo1234!", "DemoAdmin1!", 42L, historyDays);
        DemoStoryGenerator generator = new DemoStoryGenerator(KL,
                Map.of("USD", new BigDecimal("4.1650"), "SGD", new BigDecimal("3.2100")));
        DemoDataWriter writer = new DemoDataWriter(jdbc, transactionManager, passwordEncoder, properties, KL);
        DemoWarmUp warmUp = new DemoWarmUp(transferMoneyUseCase, recordForeignRemittanceUseCase);
        return new DemoDataSeeder(jdbc, generator, writer, warmUp, properties, Clock.fixed(NOW, KL));
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
