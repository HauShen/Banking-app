package com.Banking_app.transaction.application.service;

import com.Banking_app.transaction.application.exception.IdempotencyKeyConflictException;
import com.Banking_app.transaction.application.exception.TransactionAccessDeniedException;
import com.Banking_app.transaction.application.exception.TransferAccountLockedException;
import com.Banking_app.transaction.application.port.in.command.TransferMoneyCommand;
import com.Banking_app.transaction.application.port.in.result.TransferResult;
import com.Banking_app.transaction.domain.enums.LedgerType;
import com.Banking_app.transaction.domain.enums.TransactionStatus;
import com.Banking_app.transaction.domain.exception.SameAccountTransferException;
import com.Banking_app.transaction.domain.models.transaction.Transaction;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Use-case tests with fake ports: no Spring, no database. Mirrors the HTTP tests in TransferSecurityTest at the
 * application level, plus the cases those don't reach (locked accounts, order of the checks).
 */
class TransferMoneyServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-06T08:00:00Z");
    private static final Long ALICE_ACC = 1L;
    private static final Long BOB_ACC = 2L;

    private final FakeAccounts accounts = new FakeAccounts();
    private final FakeTransactionStore store = new FakeTransactionStore();
    private final TransferMoneyService service = new TransferMoneyService(accounts, store, store,
            new SequentialReferences(), Clock.fixed(NOW, ZoneOffset.UTC));

    TransferMoneyServiceTest() {
        accounts.add(ALICE_ACC, "1000000001", "alice", false, "120.00");
        accounts.add(BOB_ACC, "1000000002", "bob", false, "20.00");
    }

    private static TransferMoneyCommand command(String user, String from, String to, String amount, String key) {
        return new TransferMoneyCommand(user, from, to, new BigDecimal(amount), "Dinner", key);
    }

    private void assertNothingHappened() {
        assertThat(accounts.moveCalls).isZero();
        assertThat(store.saved).isEmpty();
        assertThat(accounts.balanceOf(ALICE_ACC)).isEqualByComparingTo("120.00");
        assertThat(accounts.balanceOf(BOB_ACC)).isEqualByComparingTo("20.00");
    }

    @Test
    @DisplayName("the owner's transfer moves the money and stores a SUCCESS transaction with DEBIT + CREDIT")
    void completesTransfer() {
        TransferResult result = service.transfer(command("alice", "1000000001", "1000000002", "30.00", "k1"));

        assertThat(result.transactionReference()).isEqualTo("TXN-000000000001");
        assertThat(result.status()).isEqualTo(TransactionStatus.SUCCESS);
        assertThat(result.createdAt()).isEqualTo(NOW);
        assertThat(result.duplicate()).isFalse();

        assertThat(accounts.balanceOf(ALICE_ACC)).isEqualByComparingTo("90.00");
        assertThat(accounts.balanceOf(BOB_ACC)).isEqualByComparingTo("50.00");

        assertThat(store.saved).hasSize(1);
        Transaction tx = store.saved.get(0);
        assertThat(tx.getFromAccountId()).isEqualTo(ALICE_ACC);
        assertThat(tx.getToAccountId()).isEqualTo(BOB_ACC);
        assertThat(tx.getIdempotencyKey()).isEqualTo("k1");
        assertThat(tx.getDescription()).isEqualTo("Dinner");
        assertThat(tx.getSuccessAt()).isEqualTo(NOW);
        assertThat(tx.getLedgerEntries()).hasSize(2);
        assertThat(tx.getLedgerEntries().get(0).type()).isEqualTo(LedgerType.DEBIT);
        assertThat(tx.getLedgerEntries().get(0).balanceAfter()).isEqualByComparingTo("90.00");
        assertThat(tx.getLedgerEntries().get(1).type()).isEqualTo(LedgerType.CREDIT);
        assertThat(tx.getLedgerEntries().get(1).balanceAfter()).isEqualByComparingTo("50.00");
    }

    @Test
    @DisplayName("same account on both sides → 400 before anything is locked")
    void sameAccount() {
        assertThatThrownBy(() -> service.transfer(command("alice", "1000000001", "1000000001", "10", "k1")))
                .isInstanceOf(SameAccountTransferException.class)
                .hasMessage("Cannot transfer to the same account");
        assertThat(accounts.lockCalls).isZero();
        assertNothingHappened();
    }

    @Nested
    @DisplayName("ownership")
    class Ownership {

        @Test
        @DisplayName("someone else's account → 403 after locking, nothing moves")
        void notOwner() {
            assertThatThrownBy(() -> service.transfer(command("bob", "1000000001", "1000000002", "10", "k1")))
                    .isInstanceOf(TransactionAccessDeniedException.class)
                    .hasMessage("You can only transfer from your own accounts");
            assertThat(accounts.lockCalls).isEqualTo(1);
            assertNothingHappened();
        }

        @Test
        @DisplayName("admins are not the owner either → 403")
        void adminIsNotOwner() {
            assertThatThrownBy(() -> service.transfer(command("admin-id", "1000000001", "1000000002", "10", "k1")))
                    .isInstanceOf(TransactionAccessDeniedException.class);
            assertNothingHappened();
        }
    }

    @Nested
    @DisplayName("idempotency")
    class Idempotency {

        @Test
        @DisplayName("same key from the same account → the original transfer, nothing moves again")
        void duplicateReturnsOriginal() {
            Instant earlier = NOW.minusSeconds(60);
            store.add("TXN-ORIGINAL0001", ALICE_ACC, BOB_ACC, "30.00", "k1", earlier);

            TransferResult result = service.transfer(command("alice", "1000000001", "1000000002", "30.00", "k1"));

            assertThat(result.duplicate()).isTrue();
            assertThat(result.transactionReference()).isEqualTo("TXN-ORIGINAL0001");
            assertThat(result.status()).isEqualTo(TransactionStatus.SUCCESS);
            assertThat(result.createdAt()).isEqualTo(earlier);
            assertNothingHappened();
        }

        @Test
        @DisplayName("the key of another account's transfer → 400, that transfer is not revealed")
        void keyOfAnotherAccount() {
            store.add("TXN-BOBS00000001", BOB_ACC, ALICE_ACC, "5.00", "bobs-key", NOW.minusSeconds(60));

            assertThatThrownBy(() -> service.transfer(command("alice", "1000000001", "1000000002", "10", "bobs-key")))
                    .isInstanceOf(IdempotencyKeyConflictException.class)
                    .hasMessage("idempotencyKey has already been used for a different transfer");
            assertNothingHappened();
        }

        @Test
        @DisplayName("the key of a cash deposit → 400 (a deposit has no from-account)")
        void keyOfCashDeposit() {
            store.add("TXN-DEPOSIT00001", null, ALICE_ACC, "50.00", "deposit-key", NOW.minusSeconds(60));

            assertThatThrownBy(() -> service.transfer(command("alice", "1000000001", "1000000002", "10", "deposit-key")))
                    .isInstanceOf(IdempotencyKeyConflictException.class);
            assertNothingHappened();
        }

        @Test
        @DisplayName("ownership is checked first: a stranger with the owner's key gets 403, not the transfer")
        void ownershipBeforeIdempotency() {
            store.add("TXN-ORIGINAL0001", ALICE_ACC, BOB_ACC, "30.00", "k1", NOW.minusSeconds(60));

            assertThatThrownBy(() -> service.transfer(command("bob", "1000000001", "1000000002", "30.00", "k1")))
                    .isInstanceOf(TransactionAccessDeniedException.class);
        }
    }

    @Nested
    @DisplayName("locked accounts")
    class Locked {

        @Test
        @DisplayName("locked from-account → 400 with the old message")
        void lockedFrom() {
            accounts.add(3L, "1000000003", "carol", true, "500.00");

            assertThatThrownBy(() -> service.transfer(command("carol", "1000000003", "1000000002", "10", "k1")))
                    .isInstanceOf(TransferAccountLockedException.class)
                    .hasMessage("Cannot process transfer with an locked account.");
            assertNothingHappened();
        }

        @Test
        @DisplayName("locked to-account → 400 with the old message")
        void lockedTo() {
            accounts.add(3L, "1000000003", "carol", true, "500.00");

            assertThatThrownBy(() -> service.transfer(command("alice", "1000000001", "1000000003", "10", "k1")))
                    .isInstanceOf(TransferAccountLockedException.class)
                    .hasMessage("Selected account has been locked");
            assertNothingHappened();
        }

        @Test
        @DisplayName("a retry of an earlier transfer still gets the original, even if the account is locked now")
        void duplicateBeforeLockedCheck() {
            accounts.add(3L, "1000000003", "carol", true, "500.00");
            store.add("TXN-ORIGINAL0003", 3L, BOB_ACC, "10.00", "k3", NOW.minusSeconds(60));

            TransferResult result = service.transfer(command("carol", "1000000003", "1000000002", "10", "k3"));

            assertThat(result.duplicate()).isTrue();
            assertThat(result.transactionReference()).isEqualTo("TXN-ORIGINAL0003");
        }
    }

    @Test
    @DisplayName("below the minimum balance → the account module's 400 passes through, nothing is stored")
    void insufficientBalance() {
        assertThatThrownBy(() -> service.transfer(command("alice", "1000000001", "1000000002", "100.01", "k1")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Insufficient balance. Minimum balance of $20.00 must be maintained.");
        assertThat(store.saved).isEmpty();
        assertThat(accounts.balanceOf(ALICE_ACC)).isEqualByComparingTo("120.00");
    }
}
