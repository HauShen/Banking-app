package com.Banking_app.transaction.adapter.out.persistence;

import com.Banking_app.support.AbstractIntegrationTest;
import com.Banking_app.transaction.domain.models.transaction.LedgerEntry;
import com.Banking_app.transaction.domain.models.transaction.Transaction;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The ledger reads behind the balance history, against the real schema. Entries are written with past dates
 * through TransactionPersistenceAdapter (they don't change any account balance: this test is only about reading
 * the ledger by time).
 */
class LedgerPersistenceAdapterTest extends AbstractIntegrationTest {

    private static final Instant DAY_1 = Instant.parse("2026-10-01T02:00:00Z");

    @Autowired
    private TransactionPersistenceAdapter transactions;

    @Autowired
    private LedgerPersistenceAdapter ledger;

    private static String unique(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    /** A cash deposit into the account, posted at {@code at}, leaving {@code balanceAfter}. */
    private void deposit(long accountId, String amount, String balanceAfter, Instant at) {
        transactions.save(Transaction.startCashDeposit(unique("TXN"), accountId, new BigDecimal(amount), "ledger test",
                        unique("key"), at)
                .completeCashDeposit(new BigDecimal(balanceAfter), at));
    }

    private static List<String> balances(List<LedgerEntry> entries) {
        return entries.stream().map(e -> e.balanceAfter().toPlainString()).toList();
    }

    @Test
    @DisplayName("findPostedSince: the account's entries from that moment on, oldest first, only this account's")
    void postedSince() {
        long alice = newCustomer("lp_alice").account().id();
        long bob = newCustomer("lp_bob").account().id();
        deposit(alice, "10.00", "30.00", DAY_1);
        deposit(alice, "20.00", "50.00", DAY_1.plus(2, ChronoUnit.DAYS));
        deposit(bob, "99.00", "119.00", DAY_1.plus(2, ChronoUnit.DAYS));
        deposit(alice, "5.00", "55.00", DAY_1.plus(1, ChronoUnit.DAYS));   // written last, posted in between

        assertThat(balances(ledger.findPostedSince(alice, DAY_1.plus(1, ChronoUnit.DAYS))))
                .containsExactly("55.00", "50.00");
        assertThat(balances(ledger.findPostedSince(alice, DAY_1))).containsExactly("30.00", "55.00", "50.00");
    }

    @Test
    @DisplayName("findLastPostedBefore: the latest entry strictly before the moment; same time → the last written")
    void lastPostedBefore() {
        long alice = newCustomer("lp_alice").account().id();
        deposit(alice, "10.00", "30.00", DAY_1);
        deposit(alice, "5.00", "35.00", DAY_1);                              // same microsecond, written later
        deposit(alice, "20.00", "55.00", DAY_1.plus(1, ChronoUnit.DAYS));

        assertThat(ledger.findLastPostedBefore(alice, DAY_1.plus(1, ChronoUnit.DAYS)))
                .map(e -> e.balanceAfter().toPlainString()).contains("35.00");
        assertThat(ledger.findLastPostedBefore(alice, DAY_1)).isEmpty();      // strictly before
    }
}
