package com.Banking_app.demo;

import com.Banking_app.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The writer against the real schema (Testcontainers PostgreSQL, all Flyway migrations).
 *
 * <p>The test database is shared by every test class, so each successful write runs inside a transaction that is
 * rolled back at the end: the demo rows never stay behind and can't disturb other tests (or the seeder test of
 * step 6, which needs a database without weiming). The writer joins that transaction instead of committing its own.
 */
class DemoDataWriterTest extends AbstractIntegrationTest {

    private static final ZoneId KL = ZoneId.of("Asia/Kuala_Lumpur");
    private static final Instant NOW = ZonedDateTime.of(LocalDate.of(2026, 11, 26), LocalTime.NOON, KL).toInstant();
    private static final DemoProperties PROPERTIES = new DemoProperties("Demo1234!", "DemoAdmin1!", 42L, 90);

    @Autowired private JdbcTemplate jdbc;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private PasswordEncoder passwordEncoder;

    private final DemoCast cast = DemoCast.standard(90);
    private final DemoStory story = new DemoStoryGenerator(KL,
            Map.of("USD", new BigDecimal("4.1650"), "SGD", new BigDecimal("3.2100")))
            .generate(cast, NOW, new Random(42));

    @Test
    void writesTheCastAndTheStory_andTheLedgerAddsUp() {
        inRolledBackTransaction(() -> {
            long outboxBefore = count("SELECT COUNT(*) FROM outbox_events");

            DemoWriteResult result = writer().write(cast, story, NOW);

            long transfers = story.events().stream().filter(e -> e.type() == DemoEvent.Type.TRANSFER).count();
            assertThat(result).isEqualTo(new DemoWriteResult(6, 7, story.events().size(),
                    (int) (story.events().size() + transfers)));
            assertThat(count("SELECT COUNT(*) FROM users WHERE email LIKE '%@banking.demo'")).isEqualTo(6);
            assertThat(count("SELECT COUNT(*) FROM outbox_events")).as("history never reaches Kafka")
                    .isEqualTo(outboxBefore);

            for (DemoAccount account : cast.accounts()) {
                assertLedgerAddsUp(account.accountNumber());
            }
            return null;
        });
    }

    @Test
    void usersAndAccounts_haveTheCastValues() {
        inRolledBackTransaction(() -> {
            writer().write(cast, story, NOW);

            Map<String, Object> weiming = jdbc.queryForMap("SELECT * FROM users WHERE username = 'weiming'");
            assertThat(weiming.get("id")).isEqualTo(cast.user("weiming").id());
            assertThat(weiming.get("role")).isEqualTo("CUSTOMER");
            assertThat(weiming.get("full_name")).isEqualTo("Lim Wei Ming");
            assertThat(passwordEncoder.matches("Demo1234!", (String) weiming.get("password"))).isTrue();

            Map<String, Object> admin = jdbc.queryForMap("SELECT * FROM users WHERE username = 'demoadmin'");
            assertThat(admin.get("role")).isEqualTo("ADMIN");
            assertThat(passwordEncoder.matches("DemoAdmin1!", (String) admin.get("password"))).isTrue();

            Map<String, Object> locked = jdbc.queryForMap(
                    "SELECT * FROM accounts WHERE account_number = ?", DemoCast.AHMAD_SAVINGS_LOCKED);
            assertThat(locked.get("account_status")).isEqualTo("LOCKED");
            assertThat(locked.get("account_type")).isEqualTo("SAVINGS");
            assertThat(locked.get("account_currency")).isEqualTo("MYR");
            assertThat(locked.get("user_id")).isEqualTo(cast.user("ahmad.f").id());

            DemoAccount main = cast.account(DemoCast.MAIN_SAVINGS);
            OffsetDateTime openedAt = jdbc.queryForObject(
                    "SELECT created_at FROM accounts WHERE account_number = ?", OffsetDateTime.class,
                    DemoCast.MAIN_SAVINGS);
            assertThat(openedAt.toInstant()).isEqualTo(main.openedAt(LocalDate.of(2026, 11, 26), KL));

            assertThat(jdbc.queryForObject("SELECT current_balance FROM accounts WHERE account_number = ?",
                    BigDecimal.class, DemoCast.JASON_SAVINGS_NEW)).isEqualByComparingTo("20.00");
            return null;
        });
    }

    @Test
    void transactions_followTheTableRules() {
        inRolledBackTransaction(() -> {
            writer().write(cast, story, NOW);

            List<Map<String, Object>> rows = jdbc.queryForList("""
                    SELECT t.* FROM transactions t JOIN accounts a ON a.id = t.to_account_id
                    WHERE a.user_id IN (SELECT id FROM users WHERE email LIKE '%@banking.demo')""");
            assertThat(rows).hasSize(story.events().size());

            for (Map<String, Object> row : rows) {
                String type = (String) row.get("transaction_type");
                String reference = (String) row.get("reference_number");
                assertThat(row.get("status")).isEqualTo("SUCCESS");
                assertThat(row.get("created_at")).isEqualTo(row.get("success_at"));
                assertThat(reference).matches(switch (type) {
                    case "TRANSFER" -> "TXN-[0-9A-F]{12}";
                    case "CASH_DEPOSIT" -> "CDM-[0-9A-F]{12}";
                    default -> "RMT-[0-9A-F]{12}";
                });
                assertThat((String) row.get("idempotency_key")).startsWith("demo-");
                assertThat(row.get("original_currency") != null).isEqualTo(type.equals("FOREIGN_REMITTANCE"));

                long ledgerRows = count("SELECT COUNT(*) FROM ledgers WHERE transaction_id = ?", row.get("id"));
                assertThat(ledgerRows).as(reference).isEqualTo(type.equals("TRANSFER") ? 2 : 1);
            }
            return null;
        });
    }

    @Test
    void sameSeed_givesTheSameReferences() {
        List<String> first = inRolledBackTransaction(this::writeAndListReferences);
        List<String> second = inRolledBackTransaction(this::writeAndListReferences);

        assertThat(first).hasSize(story.events().size()).doesNotHaveDuplicates().isEqualTo(second);
    }

    @Test
    void storyThatOverdraws_writesNothing() {
        long before = count("SELECT COUNT(*) FROM users WHERE email LIKE '%@banking.demo'");
        Instant t = NOW.minusSeconds(3600);
        DemoStory overdraws = new DemoStory(
                List.of(DemoEvent.transfer(t, DemoCast.JASON_SAVINGS_NEW, DemoCast.PRIYA_SAVINGS,
                        new BigDecimal("100.00"), "More than RM 20")),
                balancesOf(story));

        assertThatThrownBy(() -> writer().write(cast, overdraws, NOW))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("overdraws");
        assertThat(count("SELECT COUNT(*) FROM users WHERE email LIKE '%@banking.demo'")).isEqualTo(before);
    }

    @Test
    void finalBalancesThatDontMatch_writeNothing() {
        long before = count("SELECT COUNT(*) FROM users WHERE email LIKE '%@banking.demo'");
        Map<String, BigDecimal> wrong = balancesOf(story);
        wrong.put(DemoCast.MAIN_SAVINGS, new BigDecimal("1.00"));

        assertThatThrownBy(() -> writer().write(cast, new DemoStory(story.events(), wrong), NOW))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining(DemoCast.MAIN_SAVINGS);
        assertThat(count("SELECT COUNT(*) FROM users WHERE email LIKE '%@banking.demo'")).isEqualTo(before);
    }

    @Test
    void eventsOutOfOrder_writeNothing() {
        long before = count("SELECT COUNT(*) FROM users WHERE email LIKE '%@banking.demo'");
        Instant t = NOW.minusSeconds(3600);
        DemoStory outOfOrder = new DemoStory(List.of(
                DemoEvent.cashDeposit(t, DemoCast.PRIYA_SAVINGS, new BigDecimal("10.00"), "Later"),
                DemoEvent.cashDeposit(t.minusSeconds(1), DemoCast.PRIYA_SAVINGS, new BigDecimal("10.00"), "Earlier")),
                balancesOf(story));

        assertThatThrownBy(() -> writer().write(cast, outOfOrder, NOW))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("time order");
        assertThat(count("SELECT COUNT(*) FROM users WHERE email LIKE '%@banking.demo'")).isEqualTo(before);
    }

    /** Walks the account's ledger from RM 20.00 and checks every balance_after and the final current_balance. */
    private void assertLedgerAddsUp(String accountNumber) {
        Long accountId = jdbc.queryForObject("SELECT id FROM accounts WHERE account_number = ?", Long.class,
                accountNumber);
        BigDecimal currentBalance = jdbc.queryForObject("SELECT current_balance FROM accounts WHERE id = ?",
                BigDecimal.class, accountId);
        List<Map<String, Object>> ledger = jdbc.queryForList(
                "SELECT ledger_type, amount, balance_after FROM ledgers WHERE account_id = ? ORDER BY created_at, id",
                accountId);

        BigDecimal running = new BigDecimal("20.00");
        for (Map<String, Object> row : ledger) {
            BigDecimal amount = (BigDecimal) row.get("amount");
            running = "CREDIT".equals(row.get("ledger_type")) ? running.add(amount) : running.subtract(amount);
            assertThat(amount.signum()).as("ledger amounts are positive").isPositive();
            assertThat((BigDecimal) row.get("balance_after")).as(accountNumber).isEqualByComparingTo(running);
            assertThat(running.signum()).as(accountNumber).isNotNegative();
        }
        assertThat(currentBalance).as(accountNumber).isEqualByComparingTo(running)
                .isEqualByComparingTo(story.finalBalance(accountNumber));
    }

    private List<String> writeAndListReferences() {
        writer().write(cast, story, NOW);
        return jdbc.queryForList("""
                SELECT t.reference_number FROM transactions t JOIN accounts a ON a.id = t.to_account_id
                WHERE a.user_id IN (SELECT id FROM users WHERE email LIKE '%@banking.demo')
                ORDER BY t.created_at""", String.class);
    }

    private DemoDataWriter writer() {
        return new DemoDataWriter(jdbc, transactionManager, passwordEncoder, PROPERTIES, KL);
    }

    private <T> T inRolledBackTransaction(Supplier<T> work) {
        return new TransactionTemplate(transactionManager).execute(status -> {
            status.setRollbackOnly();
            return work.get();
        });
    }

    private long count(String sql, Object... args) {
        Long n = jdbc.queryForObject(sql, Long.class, args);
        return n == null ? 0 : n;
    }

    private static Map<String, BigDecimal> balancesOf(DemoStory story) {
        return new HashMap<>(story.finalBalances());
    }
}
