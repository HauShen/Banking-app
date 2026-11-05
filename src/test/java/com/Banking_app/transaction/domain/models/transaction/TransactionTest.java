package com.Banking_app.transaction.domain.models.transaction;

import com.Banking_app.transaction.domain.enums.LedgerType;
import com.Banking_app.transaction.domain.enums.TransactionStatus;
import com.Banking_app.transaction.domain.exception.InvalidTransactionAmountException;
import com.Banking_app.transaction.domain.exception.SameAccountTransferException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Plain unit tests: no Spring, no database, run in milliseconds. */
class TransactionTest {

    private static final Long ALICE = 1L;
    private static final Long BOB = 2L;
    private static final Instant CREATED = Instant.parse("2026-11-05T10:00:00Z");
    private static final Instant DONE = Instant.parse("2026-11-05T10:00:01Z");

    private static Transaction pendingTransfer(String amount) {
        return Transaction.startTransfer("TXN-AAAABBBBCCCC", ALICE, BOB, new BigDecimal(amount),
                "Dinner", "key-1", CREATED);
    }

    private static Transaction pendingCashDeposit(String amount) {
        return Transaction.startCashDeposit("TXN-DDDDEEEEFFFF", BOB, new BigDecimal(amount),
                "Cash deposit", "key-2", CREATED);
    }

    @Nested
    @DisplayName("startTransfer")
    class StartTransfer {

        @Test
        @DisplayName("creates a PENDING transfer with no ledger entries yet")
        void createsPendingTransfer() {
            Transaction tx = pendingTransfer("30.00");

            assertThat(tx.getId()).isNull();
            assertThat(tx.getReferenceNumber()).isEqualTo("TXN-AAAABBBBCCCC");
            assertThat(tx.getFromAccountId()).isEqualTo(ALICE);
            assertThat(tx.getToAccountId()).isEqualTo(BOB);
            assertThat(tx.getAmount()).isEqualByComparingTo("30.00");
            assertThat(tx.getDescription()).isEqualTo("Dinner");
            assertThat(tx.getIdempotencyKey()).isEqualTo("key-1");
            assertThat(tx.getStatus()).isEqualTo(TransactionStatus.PENDING);
            assertThat(tx.getCreatedAt()).isEqualTo(CREATED);
            assertThat(tx.getSuccessAt()).isNull();
            assertThat(tx.getLedgerEntries()).isEmpty();
            assertThat(tx.isCashDeposit()).isFalse();
        }

        @Test
        @DisplayName("rejects the same account on both sides, with the message the API returns today")
        void rejectsSameAccount() {
            assertThatThrownBy(() -> Transaction.startTransfer("TXN-1", ALICE, ALICE, new BigDecimal("10"),
                    "x", "key", CREATED))
                    .isInstanceOf(SameAccountTransferException.class)
                    .isInstanceOf(IllegalArgumentException.class)   // → 400 in GlobalExceptionHandler
                    .hasMessage("Cannot transfer to the same account");
        }

        @Test
        @DisplayName("rejects zero, negative and missing amounts")
        void rejectsNonPositiveAmount() {
            for (BigDecimal amount : new BigDecimal[]{BigDecimal.ZERO, new BigDecimal("-0.01"), null}) {
                assertThatThrownBy(() -> Transaction.startTransfer("TXN-1", ALICE, BOB, amount, "x", "key", CREATED))
                        .as("amount %s", amount)
                        .isInstanceOf(InvalidTransactionAmountException.class)
                        .hasMessage("Amount must be greater than 0");
            }
        }

        @Test
        @DisplayName("requires a reference number and an idempotency key")
        void requiresReferenceAndKey() {
            assertThatThrownBy(() -> Transaction.startTransfer(" ", ALICE, BOB, BigDecimal.TEN, "x", "key", CREATED))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("referenceNumber is required");
            assertThatThrownBy(() -> Transaction.startTransfer("TXN-1", ALICE, BOB, BigDecimal.TEN, "x", null, CREATED))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("idempotencyKey is required");
        }

        @Test
        @DisplayName("requires both account ids")
        void requiresBothAccounts() {
            assertThatThrownBy(() -> Transaction.startTransfer("TXN-1", null, BOB, BigDecimal.TEN, "x", "key", CREATED))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> Transaction.startTransfer("TXN-1", ALICE, null, BigDecimal.TEN, "x", "key", CREATED))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("completeTransfer")
    class CompleteTransfer {

        @Test
        @DisplayName("marks SUCCESS and records a DEBIT for the sender and a CREDIT for the recipient")
        void recordsDebitAndCredit() {
            Transaction done = pendingTransfer("30.00")
                    .completeTransfer(new BigDecimal("90.00"), new BigDecimal("50.00"), DONE);

            assertThat(done.getStatus()).isEqualTo(TransactionStatus.SUCCESS);
            assertThat(done.getSuccessAt()).isEqualTo(DONE);
            assertThat(done.getCreatedAt()).isEqualTo(CREATED);

            List<LedgerEntry> entries = done.getLedgerEntries();
            assertThat(entries).hasSize(2);

            LedgerEntry debit = entries.get(0);
            assertThat(debit.type()).isEqualTo(LedgerType.DEBIT);
            assertThat(debit.accountId()).isEqualTo(ALICE);
            assertThat(debit.amount()).isEqualByComparingTo("30.00");      // positive: the type says "out"
            assertThat(debit.balanceAfter()).isEqualByComparingTo("90.00");
            assertThat(debit.postedAt()).isEqualTo(DONE);

            LedgerEntry credit = entries.get(1);
            assertThat(credit.type()).isEqualTo(LedgerType.CREDIT);
            assertThat(credit.accountId()).isEqualTo(BOB);
            assertThat(credit.amount()).isEqualByComparingTo("30.00");
            assertThat(credit.balanceAfter()).isEqualByComparingTo("50.00");
        }

        @Test
        @DisplayName("returns a new object and leaves the PENDING one unchanged")
        void isImmutable() {
            Transaction pending = pendingTransfer("30.00");

            Transaction done = pending.completeTransfer(new BigDecimal("90.00"), new BigDecimal("50.00"), DONE);

            assertThat(done).isNotSameAs(pending);
            assertThat(pending.getStatus()).isEqualTo(TransactionStatus.PENDING);
            assertThat(pending.getLedgerEntries()).isEmpty();
        }

        @Test
        @DisplayName("cannot complete twice")
        void cannotCompleteTwice() {
            Transaction done = pendingTransfer("30.00")
                    .completeTransfer(new BigDecimal("90.00"), new BigDecimal("50.00"), DONE);

            assertThatThrownBy(() -> done.completeTransfer(new BigDecimal("60.00"), new BigDecimal("80.00"), DONE))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("is not allowed on a cash deposit")
        void notForCashDeposit() {
            assertThatThrownBy(() -> pendingCashDeposit("50.00")
                    .completeTransfer(new BigDecimal("90.00"), new BigDecimal("70.00"), DONE))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("requires both balances after")
        void requiresBalances() {
            assertThatThrownBy(() -> pendingTransfer("30.00").completeTransfer(null, new BigDecimal("50.00"), DONE))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("cash deposit")
    class CashDeposit {

        @Test
        @DisplayName("has no from-account")
        void hasNoSender() {
            Transaction tx = pendingCashDeposit("50.00");

            assertThat(tx.getFromAccountId()).isNull();
            assertThat(tx.getToAccountId()).isEqualTo(BOB);
            assertThat(tx.isCashDeposit()).isTrue();
            assertThat(tx.getStatus()).isEqualTo(TransactionStatus.PENDING);
        }

        @Test
        @DisplayName("completing records only a CREDIT")
        void recordsOnlyCredit() {
            Transaction done = pendingCashDeposit("50.00").completeCashDeposit(new BigDecimal("70.00"), DONE);

            assertThat(done.getStatus()).isEqualTo(TransactionStatus.SUCCESS);
            assertThat(done.getSuccessAt()).isEqualTo(DONE);
            assertThat(done.getLedgerEntries()).singleElement().satisfies(credit -> {
                assertThat(credit.type()).isEqualTo(LedgerType.CREDIT);
                assertThat(credit.accountId()).isEqualTo(BOB);
                assertThat(credit.amount()).isEqualByComparingTo("50.00");
                assertThat(credit.balanceAfter()).isEqualByComparingTo("70.00");
            });
        }

        @Test
        @DisplayName("completeCashDeposit is not allowed on a transfer")
        void notForTransfer() {
            assertThatThrownBy(() -> pendingTransfer("30.00").completeCashDeposit(new BigDecimal("50.00"), DONE))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    @Nested
    @DisplayName("questions")
    class Questions {

        @Test
        @DisplayName("isSentFrom: only the sender, never for a cash deposit")
        void isSentFrom() {
            assertThat(pendingTransfer("10").isSentFrom(ALICE)).isTrue();
            assertThat(pendingTransfer("10").isSentFrom(BOB)).isFalse();
            assertThat(pendingTransfer("10").isSentFrom(null)).isFalse();
            assertThat(pendingCashDeposit("10").isSentFrom(BOB)).isFalse();
        }

        @Test
        @DisplayName("involves: sender or recipient")
        void involves() {
            assertThat(pendingTransfer("10").involves(ALICE)).isTrue();
            assertThat(pendingTransfer("10").involves(BOB)).isTrue();
            assertThat(pendingTransfer("10").involves(3L)).isFalse();
            assertThat(pendingCashDeposit("10").involves(BOB)).isTrue();
        }
    }

    @Test
    @DisplayName("the ledger entry list cannot be changed from outside")
    void ledgerEntriesAreUnmodifiable() {
        Transaction done = pendingTransfer("30.00")
                .completeTransfer(new BigDecimal("90.00"), new BigDecimal("50.00"), DONE);

        assertThatThrownBy(() -> done.getLedgerEntries().clear())
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("rehydrate rebuilds a stored transaction exactly")
    void rehydrate() {
        LedgerEntry credit = new LedgerEntry(11L, BOB, LedgerType.CREDIT, new BigDecimal("50.00"),
                new BigDecimal("70.00"), DONE);

        Transaction tx = Transaction.rehydrate(7L, "TXN-DDDDEEEEFFFF", null, BOB, new BigDecimal("50.00"),
                "Cash deposit", TransactionStatus.SUCCESS, CREATED, DONE, "key-2", List.of(credit));

        assertThat(tx.getId()).isEqualTo(7L);
        assertThat(tx.getStatus()).isEqualTo(TransactionStatus.SUCCESS);
        assertThat(tx.getSuccessAt()).isEqualTo(DONE);
        assertThat(tx.isCashDeposit()).isTrue();
        assertThat(tx.getLedgerEntries()).containsExactly(credit);
    }
}
