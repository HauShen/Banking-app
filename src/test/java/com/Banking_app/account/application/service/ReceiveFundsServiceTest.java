package com.Banking_app.account.application.service;

import com.Banking_app.account.application.exception.TransferAccountNotFoundException;
import com.Banking_app.account.application.port.in.result.AccountSnapshot;
import com.Banking_app.account.domain.enums.AccountStatus;
import com.Banking_app.account.domain.exception.AccountNotActiveException;
import com.Banking_app.account.domain.exception.InvalidAmountException;
import com.Banking_app.exception.ResourceNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Use-case test with a fake store (no Spring, no database). The @Transactional(MANDATORY) rule is a Spring
 * annotation and is not active here; ReceiveFundsUseCaseIntegrationTest covers it.
 */
class ReceiveFundsServiceTest {

    private final InMemoryAccountStore store = new InMemoryAccountStore();
    private final ReceiveFundsService service = new ReceiveFundsService(store, store);

    private static final Long BOB_ACCOUNT = 2L;

    @Nested
    @DisplayName("lockForReceiving")
    class LockForReceiving {

        @Test
        @DisplayName("locks the account and returns a read-only snapshot, locked flag included")
        void returnsSnapshot() {
            store.add(BOB_ACCOUNT, "bob", "1000000002", AccountStatus.LOCKED, "20.00");

            AccountSnapshot snapshot = service.lockForReceiving("1000000002");

            assertThat(snapshot.id()).isEqualTo(BOB_ACCOUNT);
            assertThat(snapshot.ownerId()).isEqualTo("bob");
            assertThat(snapshot.balance()).isEqualByComparingTo("20.00");
            assertThat(snapshot.isLocked()).isTrue();
            assertThat(store.lockedAccountNumbers).containsExactly("1000000002");
        }

        @Test
        @DisplayName("unknown account number → 404 'To account not found'")
        void unknown() {
            assertThatThrownBy(() -> service.lockForReceiving("1000000009"))
                    .isInstanceOf(TransferAccountNotFoundException.class)
                    .isInstanceOf(ResourceNotFoundException.class)   // → 404
                    .hasMessage("To account not found");
        }
    }

    @Nested
    @DisplayName("receive")
    class Receive {

        @Test
        @DisplayName("adds the amount, saves and returns the balance after")
        void receives() {
            store.add(BOB_ACCOUNT, "bob", "1000000002", AccountStatus.ACTIVE, "20.00");

            BigDecimal balanceAfter = service.receive(BOB_ACCOUNT, new BigDecimal("305.00"));

            assertThat(balanceAfter).isEqualByComparingTo("325.00");
            assertThat(store.balanceOf(BOB_ACCOUNT)).isEqualByComparingTo("325.00");
            assertThat(store.saveCount).isEqualTo(1);
        }

        @Test
        @DisplayName("a locked account is refused and nothing is saved (the remittance service checks this first)")
        void locked() {
            store.add(BOB_ACCOUNT, "bob", "1000000002", AccountStatus.LOCKED, "20.00");

            assertThatThrownBy(() -> service.receive(BOB_ACCOUNT, new BigDecimal("305.00")))
                    .isInstanceOf(AccountNotActiveException.class);
            assertThat(store.balanceOf(BOB_ACCOUNT)).isEqualByComparingTo("20.00");
            assertThat(store.saveCount).isZero();
        }

        @Test
        @DisplayName("a bad amount is refused by Account.receive and nothing is saved")
        void badAmount() {
            store.add(BOB_ACCOUNT, "bob", "1000000002", AccountStatus.ACTIVE, "20.00");

            assertThatThrownBy(() -> service.receive(BOB_ACCOUNT, new BigDecimal("0.001")))
                    .isInstanceOf(InvalidAmountException.class);
            assertThat(store.saveCount).isZero();
        }

        @Test
        @DisplayName("unknown account id → 404")
        void unknown() {
            assertThatThrownBy(() -> service.receive(99L, BigDecimal.TEN))
                    .isInstanceOf(TransferAccountNotFoundException.class)
                    .hasMessage("To account not found");
        }
    }
}
