package com.Banking_app.account.application.service;

import com.Banking_app.account.application.exception.TransferAccountNotFoundException;
import com.Banking_app.account.application.port.in.result.BalancesAfter;
import com.Banking_app.account.application.port.in.result.TransferAccounts;
import com.Banking_app.account.domain.enums.AccountStatus;
import com.Banking_app.account.domain.exception.AccountNotActiveException;
import com.Banking_app.account.domain.exception.InsufficientBalanceException;
import com.Banking_app.exception.ResourceNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Use-case test with a fake store (no Spring, no database). The @Transactional(MANDATORY) rule is a Spring
 * annotation and is not active here; the HTTP tests cover the real transaction once the transfer uses this.
 */
class MoveFundsServiceTest {

    private final InMemoryAccountStore store = new InMemoryAccountStore();
    private final MoveFundsService service = new MoveFundsService(store, store);

    private static final Long ALICE_ACCOUNT = 1L;
    private static final Long BOB_ACCOUNT = 2L;

    @Nested
    @DisplayName("lockPairForTransfer")
    class LockPair {

        @Test
        @DisplayName("returns both accounts as read-only snapshots")
        void returnsSnapshots() {
            store.add(ALICE_ACCOUNT, "alice", "1000000001", AccountStatus.ACTIVE, "120.00");
            store.add(BOB_ACCOUNT, "bob", "1000000002", AccountStatus.LOCKED, "20.00");

            TransferAccounts pair = service.lockPairForTransfer("1000000001", "1000000002");

            assertThat(pair.from().id()).isEqualTo(ALICE_ACCOUNT);
            assertThat(pair.from().ownerId()).isEqualTo("alice");
            assertThat(pair.from().balance()).isEqualByComparingTo("120.00");
            assertThat(pair.from().isLocked()).isFalse();
            assertThat(pair.to().id()).isEqualTo(BOB_ACCOUNT);
            assertThat(pair.to().isLocked()).isTrue();
        }

        @Test
        @DisplayName("locks the lower account number first, whichever side it is on (no A→B / B→A deadlock)")
        void locksLowerNumberFirst() {
            store.add(ALICE_ACCOUNT, "alice", "1000000001", AccountStatus.ACTIVE, "120.00");
            store.add(BOB_ACCOUNT, "bob", "1000000002", AccountStatus.ACTIVE, "120.00");

            service.lockPairForTransfer("1000000001", "1000000002");
            assertThat(store.lockedAccountNumbers).containsExactly("1000000001", "1000000002");

            store.lockedAccountNumbers.clear();
            service.lockPairForTransfer("1000000002", "1000000001");
            assertThat(store.lockedAccountNumbers).containsExactly("1000000001", "1000000002");
        }

        @Test
        @DisplayName("unknown from-account → 404 'From account not found'")
        void unknownFrom() {
            store.add(BOB_ACCOUNT, "bob", "1000000002", AccountStatus.ACTIVE, "20.00");

            assertThatThrownBy(() -> service.lockPairForTransfer("1000000009", "1000000002"))
                    .isInstanceOf(TransferAccountNotFoundException.class)
                    .isInstanceOf(ResourceNotFoundException.class)   // → 404
                    .hasMessage("From account not found");
        }

        @Test
        @DisplayName("unknown to-account → 404 'To account not found'")
        void unknownTo() {
            store.add(ALICE_ACCOUNT, "alice", "1000000001", AccountStatus.ACTIVE, "120.00");

            assertThatThrownBy(() -> service.lockPairForTransfer("1000000001", "1000000009"))
                    .isInstanceOf(TransferAccountNotFoundException.class)
                    .hasMessage("To account not found");
        }

        @Test
        @DisplayName("both unknown → the message of whichever is looked up first (lower number), as before")
        void bothUnknown() {
            assertThatThrownBy(() -> service.lockPairForTransfer("1000000008", "1000000009"))
                    .hasMessage("From account not found");
            assertThatThrownBy(() -> service.lockPairForTransfer("1000000009", "1000000008"))
                    .hasMessage("To account not found");
        }

        @Test
        @DisplayName("the same account on both sides is rejected before any lock")
        void sameAccount() {
            assertThatThrownBy(() -> service.lockPairForTransfer("1000000001", "1000000001"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Cannot transfer to the same account");
            assertThat(store.lockedAccountNumbers).isEmpty();
        }
    }

    @Nested
    @DisplayName("move")
    class Move {

        @Test
        @DisplayName("withdraws, deposits, saves both and returns the balances after")
        void movesMoney() {
            store.add(ALICE_ACCOUNT, "alice", "1000000001", AccountStatus.ACTIVE, "120.00");
            store.add(BOB_ACCOUNT, "bob", "1000000002", AccountStatus.ACTIVE, "20.00");

            BalancesAfter after = service.move(ALICE_ACCOUNT, BOB_ACCOUNT, new BigDecimal("30.00"));

            assertThat(after.fromBalanceAfter()).isEqualByComparingTo("90.00");
            assertThat(after.toBalanceAfter()).isEqualByComparingTo("50.00");
            assertThat(store.balanceOf(ALICE_ACCOUNT)).isEqualByComparingTo("90.00");
            assertThat(store.balanceOf(BOB_ACCOUNT)).isEqualByComparingTo("50.00");
            assertThat(store.saveCount).isEqualTo(2);
        }

        @Test
        @DisplayName("below the minimum balance → 400 with today's message, nothing saved")
        void insufficientBalance() {
            store.add(ALICE_ACCOUNT, "alice", "1000000001", AccountStatus.ACTIVE, "120.00");
            store.add(BOB_ACCOUNT, "bob", "1000000002", AccountStatus.ACTIVE, "20.00");

            assertThatThrownBy(() -> service.move(ALICE_ACCOUNT, BOB_ACCOUNT, new BigDecimal("100.01")))
                    .isInstanceOf(InsufficientBalanceException.class)
                    .hasMessage("Insufficient balance. Minimum balance of $20.00 must be maintained.");
            assertThat(store.balanceOf(ALICE_ACCOUNT)).isEqualByComparingTo("120.00");
            assertThat(store.balanceOf(BOB_ACCOUNT)).isEqualByComparingTo("20.00");
            assertThat(store.saveCount).isZero();
        }

        @Test
        @DisplayName("a locked recipient is refused and nothing is saved (the transfer service checks this first)")
        void lockedRecipient() {
            store.add(ALICE_ACCOUNT, "alice", "1000000001", AccountStatus.ACTIVE, "120.00");
            store.add(BOB_ACCOUNT, "bob", "1000000002", AccountStatus.LOCKED, "20.00");

            assertThatThrownBy(() -> service.move(ALICE_ACCOUNT, BOB_ACCOUNT, new BigDecimal("30.00")))
                    .isInstanceOf(AccountNotActiveException.class);
            assertThat(store.balanceOf(ALICE_ACCOUNT)).isEqualByComparingTo("120.00");
            assertThat(store.saveCount).isZero();
        }

        @Test
        @DisplayName("unknown account id → 404")
        void unknownAccount() {
            store.add(ALICE_ACCOUNT, "alice", "1000000001", AccountStatus.ACTIVE, "120.00");

            assertThatThrownBy(() -> service.move(ALICE_ACCOUNT, 99L, new BigDecimal("30.00")))
                    .isInstanceOf(TransferAccountNotFoundException.class)
                    .hasMessage("To account not found");
        }
    }

    @Test
    @DisplayName("move re-reads the rows locked by lockPairForTransfer (no new lock order to worry about)")
    void moveUsesLockedRows() {
        store.add(ALICE_ACCOUNT, "alice", "1000000001", AccountStatus.ACTIVE, "120.00");
        store.add(BOB_ACCOUNT, "bob", "1000000002", AccountStatus.ACTIVE, "120.00");

        // Bob (…02) sends to Alice (…01): the pair is locked lower number first, then move loads from, to.
        TransferAccounts pair = service.lockPairForTransfer("1000000002", "1000000001");
        service.move(pair.from().id(), pair.to().id(), new BigDecimal("10.00"));

        assertThat(store.lockedAccountNumbers)
                .containsExactly("1000000001", "1000000002", "1000000002", "1000000001");
        assertThat(List.of(store.balanceOf(ALICE_ACCOUNT), store.balanceOf(BOB_ACCOUNT)))
                .containsExactly(new BigDecimal("130.00"), new BigDecimal("110.00"));
    }
}
