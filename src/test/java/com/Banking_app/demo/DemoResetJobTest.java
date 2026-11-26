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
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The nightly reset against the real schema. The successful reset runs inside a transaction that is rolled back:
 * PostgreSQL's TRUNCATE is transactional, so afterwards the shared test database is exactly as before, the other
 * test classes' rows included. The failure test needs no such wrapper: a failed reset rolls itself back, which is
 * what it checks.
 */
class DemoResetJobTest extends AbstractIntegrationTest {

    private static final ZoneId KL = ZoneId.of("Asia/Kuala_Lumpur");
    private static final Set<String> SEEDED_TABLES = Set.of("users", "accounts", "transactions", "ledgers",
            "outbox_events");

    @Autowired private JdbcTemplate jdbc;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private TransferMoneyUseCase transferMoneyUseCase;
    @Autowired private RecordForeignRemittanceUseCase recordForeignRemittanceUseCase;
    @Autowired private EntityManager entityManager;

    @Test
    void reset_undoesTheVisitorsDay_andSeedsTheSameDemoAgain() {
        inRolledBackTransaction(() -> {
            DemoDataSeeder seeder = seeder();
            DemoResetJob job = new DemoResetJob(jdbc, transactionManager, seeder);
            seeder.seedIfEmpty();
            List<String> historyBefore = historyReferences();

            // A visitor's day: a new customer, weiming's savings locked by demoadmin.
            jdbc.update("""
                    INSERT INTO users (id, username, role, full_name, email, password, created_at, updated_at)
                    VALUES (?, 'visitor', 'CUSTOMER', 'A Visitor', 'visitor@example.com', 'x', now(), now())""",
                    UUID.randomUUID().toString());
            jdbc.update("UPDATE accounts SET account_status = 'LOCKED' WHERE account_number = ?",
                    DemoCast.MAIN_SAVINGS);
            // In the live app every request has its own JPA session; here one session spans everything, so push
            // its pending writes out and forget its entities before the tables are emptied under it.
            entityManager.flush();
            entityManager.clear();

            job.reset();
            entityManager.flush();

            assertThat(jdbc.queryForList("SELECT username FROM users", String.class))
                    .containsExactlyInAnyOrder("weiming", "mei.ling", "ahmad.f", "priya.n", "jason.k", "demoadmin");
            assertThat(jdbc.queryForObject("SELECT account_status FROM accounts WHERE account_number = ?",
                    String.class, DemoCast.MAIN_SAVINGS)).isEqualTo("ACTIVE");
            // Same seed, same day: the same history again (it can only have grown at the end, if a story event's
            // time passed between the two runs).
            assertThat(historyReferences()).startsWith(historyBefore.toArray(String[]::new));
            assertThat(count("SELECT COUNT(*) FROM outbox_events")).as("only the new warm-up").isEqualTo(4);
            assertThat(count("SELECT MIN(id) FROM accounts")).as("ids restarted").isEqualTo(1);

            for (String table : job.tablesToEmpty()) {
                if (!SEEDED_TABLES.contains(table)) {
                    assertThat(count("SELECT COUNT(*) FROM \"" + table + "\"")).as(table).isZero();
                }
            }
            return null;
        });
    }

    @Test
    void tablesToEmpty_isEveryTableExceptFlywaysHistory() {
        DemoResetJob job = new DemoResetJob(jdbc, transactionManager, mock(DemoDataSeeder.class));

        assertThat(job.tablesToEmpty())
                .contains("users", "accounts", "transactions", "ledgers", "audit_events", "user_token_versions",
                        "refresh_tokens", "fx_quotes", "outbox_events", "transaction_alerts", "alert_transfer_log",
                        "notifications", "processed_events")
                .doesNotContain(DemoResetJob.FLYWAY_HISTORY);
    }

    @Test
    void failedSeeding_keepsYesterdaysData() {
        long usersBefore = count("SELECT COUNT(*) FROM users");
        DemoDataSeeder failing = mock(DemoDataSeeder.class);
        when(failing.seedHistoryIfEmpty()).thenThrow(new IllegalStateException("writer bug"));
        DemoResetJob job = new DemoResetJob(jdbc, transactionManager, failing);

        assertThatThrownBy(job::reset).hasMessage("writer bug");

        assertThat(count("SELECT COUNT(*) FROM users")).as("TRUNCATE rolled back").isEqualTo(usersBefore)
                .isPositive();   // at least the bootstrap test admin
        verify(failing, never()).warmUp();

        // The scheduled entry point logs the failure instead of throwing it at the scheduler.
        assertThatCode(job::nightlyReset).doesNotThrowAnyException();
        assertThat(count("SELECT COUNT(*) FROM users")).isEqualTo(usersBefore);
    }

    private List<String> historyReferences() {
        return jdbc.queryForList("""
                SELECT reference_number FROM transactions WHERE idempotency_key LIKE 'demo-%'
                ORDER BY created_at, id""", String.class);
    }

    private DemoDataSeeder seeder() {
        DemoProperties properties = new DemoProperties("Demo1234!", "DemoAdmin1!", 42L, 90);
        DemoStoryGenerator generator = new DemoStoryGenerator(KL,
                Map.of("USD", new BigDecimal("4.1650"), "SGD", new BigDecimal("3.2100")));
        DemoDataWriter writer = new DemoDataWriter(jdbc, transactionManager, passwordEncoder, properties, KL);
        DemoWarmUp warmUp = new DemoWarmUp(transferMoneyUseCase, recordForeignRemittanceUseCase);
        return new DemoDataSeeder(jdbc, generator, writer, warmUp, properties, Clock.system(KL));
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
