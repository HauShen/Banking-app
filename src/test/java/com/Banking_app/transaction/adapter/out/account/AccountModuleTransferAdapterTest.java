package com.Banking_app.transaction.adapter.out.account;

import com.Banking_app.account.application.port.in.result.AccountSnapshot;
import com.Banking_app.account.application.port.in.result.BalancesAfter;
import com.Banking_app.account.application.port.in.result.TransferAccounts;
import com.Banking_app.account.application.port.in.usecase.MoveFundsUseCase;
import com.Banking_app.account.domain.enums.AccountCurrency;
import com.Banking_app.account.domain.enums.AccountStatus;
import com.Banking_app.transaction.application.port.out.TransferBalances;
import com.Banking_app.transaction.application.port.out.TransferParties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/** Mapping only: the adapter turns the account module's records into the transaction module's records. */
class AccountModuleTransferAdapterTest {

    private BigDecimal movedAmount;

    private final MoveFundsUseCase fakeAccountModule = new MoveFundsUseCase() {
        @Override
        public TransferAccounts lockPairForTransfer(String fromAccountNumber, String toAccountNumber) {
            return new TransferAccounts(
                    new AccountSnapshot(1L, fromAccountNumber, "alice", AccountStatus.ACTIVE, AccountCurrency.MYR,
                            new BigDecimal("120.00")),
                    new AccountSnapshot(2L, toAccountNumber, "bob", AccountStatus.LOCKED, AccountCurrency.MYR,
                            new BigDecimal("20.00")));
        }

        @Override
        public BalancesAfter move(Long fromAccountId, Long toAccountId, BigDecimal amount) {
            movedAmount = amount;
            return new BalancesAfter(new BigDecimal("90.00"), new BigDecimal("50.00"));
        }
    };

    private final AccountModuleTransferAdapter adapter = new AccountModuleTransferAdapter(fakeAccountModule);

    @Test
    @DisplayName("lockPair: ids, numbers and owners are copied; LOCKED becomes locked = true")
    void lockPair() {
        TransferParties parties = adapter.lockPair("1000000001", "1000000002");

        assertThat(parties.from().accountId()).isEqualTo(1L);
        assertThat(parties.from().accountNumber()).isEqualTo("1000000001");
        assertThat(parties.from().ownerId()).isEqualTo("alice");
        assertThat(parties.from().locked()).isFalse();
        assertThat(parties.to().accountId()).isEqualTo(2L);
        assertThat(parties.to().ownerId()).isEqualTo("bob");
        assertThat(parties.to().locked()).isTrue();
    }

    @Test
    @DisplayName("move: passes the amount on and returns both balances after")
    void move() {
        TransferBalances balances = adapter.move(1L, 2L, new BigDecimal("30.00"));

        assertThat(movedAmount).isEqualByComparingTo("30.00");
        assertThat(balances.fromBalanceAfter()).isEqualByComparingTo("90.00");
        assertThat(balances.toBalanceAfter()).isEqualByComparingTo("50.00");
    }
}
