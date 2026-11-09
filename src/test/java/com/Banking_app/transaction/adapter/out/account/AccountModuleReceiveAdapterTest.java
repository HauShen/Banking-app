package com.Banking_app.transaction.adapter.out.account;

import com.Banking_app.account.application.port.in.result.AccountSnapshot;
import com.Banking_app.account.application.port.in.usecase.ReceiveFundsUseCase;
import com.Banking_app.account.domain.enums.AccountCurrency;
import com.Banking_app.account.domain.enums.AccountStatus;
import com.Banking_app.transaction.application.port.out.ReceivingAccount;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/** Mapping only: the adapter turns the account module's records into the transaction module's records. */
class AccountModuleReceiveAdapterTest {

    private BigDecimal receivedAmount;

    private final ReceiveFundsUseCase fakeAccountModule = new ReceiveFundsUseCase() {
        @Override
        public AccountSnapshot lockForReceiving(String accountNumber) {
            return new AccountSnapshot(2L, accountNumber, "bob", AccountStatus.LOCKED, AccountCurrency.MYR,
                    new BigDecimal("20.00"));
        }

        @Override
        public BigDecimal receive(Long accountId, BigDecimal amount) {
            receivedAmount = amount;
            return new BigDecimal("480.00");
        }
    };

    private final AccountModuleReceiveAdapter adapter = new AccountModuleReceiveAdapter(fakeAccountModule);

    @Test
    @DisplayName("lockForReceiving: id and number are copied; LOCKED becomes locked = true")
    void lock() {
        ReceivingAccount account = adapter.lockForReceiving("1000000002");

        assertThat(account.accountId()).isEqualTo(2L);
        assertThat(account.accountNumber()).isEqualTo("1000000002");
        assertThat(account.locked()).isTrue();
    }

    @Test
    @DisplayName("receive passes the MYR amount on and returns the balance after")
    void receive() {
        BigDecimal balanceAfter = adapter.receive(2L, new BigDecimal("460.00"));

        assertThat(receivedAmount).isEqualByComparingTo("460.00");
        assertThat(balanceAfter).isEqualByComparingTo("480.00");
    }
}
