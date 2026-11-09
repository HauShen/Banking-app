package com.Banking_app.transaction.adapter.out.persistence;

import com.Banking_app.shared.PageResult;
import com.Banking_app.support.AbstractIntegrationTest;
import com.Banking_app.transaction.application.port.out.DuplicateIdempotencyKeyException;
import com.Banking_app.transaction.application.port.out.TransactionSummary;
import com.Banking_app.transaction.domain.enums.TransactionStatus;
import com.Banking_app.transaction.domain.models.transaction.Transaction;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The new persistence adapter against the real PostgreSQL schema (Flyway migrations, ddl-auto=validate).
 * Accounts come from the HTTP helpers, so the foreign keys are real. The rows written here don't change any
 * balance: this test is only about storing and reading transactions.
 */
class TransactionPersistenceAdapterTest extends AbstractIntegrationTest {

    @Autowired
    private TransactionPersistenceAdapter adapter;

    @Autowired
    private JdbcTemplate jdbc;

    /** PostgreSQL keeps microseconds, so test times are truncated to micros to compare exactly. */
    private static Instant now() {
        return Instant.now().truncatedTo(ChronoUnit.MICROS);
    }

    private static String unique(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    private static Transaction completedTransfer(long fromId, long toId, String amount, Instant at) {
        return completedTransfer(fromId, toId, amount, at, unique("key"));
    }

    private static Transaction completedTransfer(long fromId, long toId, String amount, Instant at, String key) {
        return Transaction.startTransfer(unique("TXN"), fromId, toId, new BigDecimal(amount), "adapter test",
                        key, at)
                .completeTransfer(new BigDecimal("100.00"), new BigDecimal("50.00"), at.plusMillis(5));
    }

    private List<Map<String, Object>> ledgerRowsOf(Long transactionId) {
        return jdbc.queryForList(
                "SELECT account_id, ledger_type, amount, balance_after FROM ledgers WHERE transaction_id = ? ORDER BY id",
                transactionId);
    }

    @Test
    @DisplayName("save stores the transfer and both ledger entries; the reads find it by reference and by key")
    void savesAndReadsTransfer() {
        Customer alice = newCustomer("pa_alice");
        Customer bob = newCustomer("pa_bob");
        Instant at = now();

        Transaction saved = adapter.save(completedTransfer(alice.account().id(), bob.account().id(), "30.00", at));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getLedgerEntries()).hasSize(2);
        assertThat(saved.getLedgerEntries()).allSatisfy(entry -> assertThat(entry.id()).isNotNull());

        List<Map<String, Object>> ledgers = ledgerRowsOf(saved.getId());
        assertThat(ledgers).hasSize(2);
        assertThat(ledgers.get(0).get("ledger_type")).isEqualTo("DEBIT");
        assertThat(((Number) ledgers.get(0).get("account_id")).longValue()).isEqualTo(alice.account().id());
        assertThat((BigDecimal) ledgers.get(0).get("balance_after")).isEqualByComparingTo("100.00");
        assertThat(ledgers.get(1).get("ledger_type")).isEqualTo("CREDIT");
        assertThat(((Number) ledgers.get(1).get("account_id")).longValue()).isEqualTo(bob.account().id());
        assertThat((BigDecimal) ledgers.get(1).get("amount")).isEqualByComparingTo("30.00");

        TransactionSummary byReference = adapter.findByReferenceNumber(saved.getReferenceNumber()).orElseThrow();
        assertThat(byReference.id()).isEqualTo(saved.getId());
        assertThat(byReference.fromAccountId()).isEqualTo(alice.account().id());
        assertThat(byReference.toAccountId()).isEqualTo(bob.account().id());
        assertThat(byReference.amount()).isEqualByComparingTo("30.00");
        assertThat(byReference.description()).isEqualTo("adapter test");
        assertThat(byReference.status()).isEqualTo(TransactionStatus.SUCCESS);
        assertThat(byReference.createdAt()).isEqualTo(at);
        assertThat(byReference.successAt()).isEqualTo(at.plusMillis(5));

        assertThat(adapter.findByIdempotencyKey(saved.getIdempotencyKey()).orElseThrow().referenceNumber())
                .isEqualTo(saved.getReferenceNumber());
    }

    @Test
    @DisplayName("rows written by the adapter are read correctly through GET /api/transfers/{reference}")
    void endpointReadsAdapterRows() {
        Customer alice = newCustomer("pa_alice");
        Customer bob = newCustomer("pa_bob");
        Transaction saved = adapter.save(completedTransfer(alice.account().id(), bob.account().id(), "12.50", now()));

        ResponseEntity<String> response = get("/api/transfers/" + saved.getReferenceNumber(), alice.token());

        assertStatus(response, 200);
        assertThat(text(response, "fromAccountNumber")).isEqualTo(alice.account().number());
        assertThat(text(response, "toAccountNumber")).isEqualTo(bob.account().number());
        assertThat(json(response).get("amount").decimalValue()).isEqualByComparingTo("12.50");
        assertThat(text(response, "status")).isEqualTo("SUCCESS");
    }

    @Test
    @DisplayName("a cash deposit is stored without a from-account and with a single CREDIT entry")
    void savesCashDeposit() {
        Customer bob = newCustomer("pa_bob");
        Instant at = now();
        Transaction deposit = Transaction.startCashDeposit(unique("TXN"), bob.account().id(), new BigDecimal("50.00"),
                "Cash deposit", unique("key"), at).completeCashDeposit(new BigDecimal("70.00"), at);

        Transaction saved = adapter.save(deposit);

        TransactionSummary summary = adapter.findByReferenceNumber(saved.getReferenceNumber()).orElseThrow();
        assertThat(summary.fromAccountId()).isNull();
        assertThat(summary.toAccountId()).isEqualTo(bob.account().id());
        List<Map<String, Object>> ledgers = ledgerRowsOf(saved.getId());
        assertThat(ledgers).hasSize(1);
        assertThat(ledgers.get(0).get("ledger_type")).isEqualTo("CREDIT");
    }

    @Test
    @DisplayName("an account's page: only its transactions, newest first, with correct paging numbers")
    void pageInvolvingAccount() {
        long a = newCustomer("pa_a").account().id();
        long b = newCustomer("pa_b").account().id();
        long c = newCustomer("pa_c").account().id();
        Instant t0 = now();
        Transaction ab = adapter.save(completedTransfer(a, b, "1.00", t0));
        Transaction bc = adapter.save(completedTransfer(b, c, "2.00", t0.plusSeconds(60)));
        adapter.save(completedTransfer(c, a, "3.00", t0.plusSeconds(120)));                   // b not involved
        Transaction cb = adapter.save(completedTransfer(c, b, "4.00", t0.plusSeconds(180)));

        PageResult<TransactionSummary> first = adapter.findPageInvolvingAccount(b, 0, 2);
        PageResult<TransactionSummary> second = adapter.findPageInvolvingAccount(b, 1, 2);

        assertThat(first.content().stream().map(TransactionSummary::referenceNumber).toList())
                .containsExactly(cb.getReferenceNumber(), bc.getReferenceNumber());
        assertThat(first.totalElements()).isEqualTo(3L);
        assertThat(first.page()).isZero();
        assertThat(first.size()).isEqualTo(2);
        assertThat(second.content().stream().map(TransactionSummary::referenceNumber).toList())
                .containsExactly(ab.getReferenceNumber());
    }

    @Test
    @DisplayName("existsInvolvingAccount: false for a fresh account, true for sender, recipient and deposit recipient")
    void existsInvolvingAccount() {
        long sender = newCustomer("pa_s").account().id();
        long recipient = newCustomer("pa_r").account().id();
        long depositOnly = newCustomer("pa_d").account().id();
        long untouched = newCustomer("pa_u").account().id();
        Instant at = now();
        adapter.save(completedTransfer(sender, recipient, "5.00", at));
        adapter.save(Transaction.startCashDeposit(unique("TXN"), depositOnly, new BigDecimal("5.00"), "Cash deposit",
                unique("key"), at).completeCashDeposit(new BigDecimal("25.00"), at));

        assertThat(adapter.existsInvolvingAccount(sender)).isTrue();
        assertThat(adapter.existsInvolvingAccount(recipient)).isTrue();
        assertThat(adapter.existsInvolvingAccount(depositOnly)).isTrue();
        assertThat(adapter.existsInvolvingAccount(untouched)).isFalse();
    }

    @Test
    @DisplayName("a second transaction with the same idempotency key → DuplicateIdempotencyKeyException, not stored")
    void duplicateIdempotencyKey() {
        long a = newCustomer("pa_a").account().id();
        long b = newCustomer("pa_b").account().id();
        long c = newCustomer("pa_c").account().id();
        String key = unique("key");
        adapter.save(completedTransfer(a, b, "1.00", now(), key));

        assertThatThrownBy(() -> adapter.save(completedTransfer(c, b, "2.00", now(), key)))
                .isInstanceOf(DuplicateIdempotencyKeyException.class);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM transactions WHERE idempotency_key = ?",
                Integer.class, key)).isEqualTo(1);
    }

    @Test
    @DisplayName("unknown reference / key → empty; invalid paging → IllegalArgumentException (400), as before")
    void edgeCases() {
        assertThat(adapter.findByReferenceNumber("TXN-DOES-NOT-EXIST")).isEmpty();
        assertThat(adapter.findByIdempotencyKey("no-such-key")).isEmpty();
        assertThatThrownBy(() -> adapter.findPageInvolvingAccount(1L, -1, 10))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> adapter.findPageInvolvingAccount(1L, 0, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
