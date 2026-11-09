package com.Banking_app.account;

import com.Banking_app.account.application.port.in.result.AccountSnapshot;
import com.Banking_app.account.application.port.in.usecase.ReceiveFundsUseCase;
import com.Banking_app.support.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The internal ReceiveFundsUseCase wired by Spring against PostgreSQL, called the way the remittance service will
 * call it (step 6): lock, then receive, in one transaction. Balances are read through the account endpoint.
 */
class ReceiveFundsUseCaseIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private ReceiveFundsUseCase receiveFundsUseCase;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private TransactionTemplate inTransaction() {
        return new TransactionTemplate(transactionManager);
    }

    @Test
    @DisplayName("lock + receive in one transaction credits the account and returns the new balance")
    void receives() {
        Customer bob = newCustomer("recv_bob");

        BigDecimal balanceAfter = inTransaction().execute(status -> {
            AccountSnapshot account = receiveFundsUseCase.lockForReceiving(bob.account().number());
            return receiveFundsUseCase.receive(account.id(), new BigDecimal("305.00"));
        });

        assertThat(balanceAfter).isEqualByComparingTo("325.00");
        assertThat(balanceOf(bob.account())).isEqualByComparingTo("325.00");
    }

    @Test
    @DisplayName("a rolled-back transaction leaves the balance as it was")
    void rollsBack() {
        Customer bob = newCustomer("recv_bob");

        inTransaction().executeWithoutResult(status -> {
            AccountSnapshot account = receiveFundsUseCase.lockForReceiving(bob.account().number());
            receiveFundsUseCase.receive(account.id(), new BigDecimal("305.00"));
            status.setRollbackOnly();   // e.g. the remittance's insert failed afterwards
        });

        assertThat(balanceOf(bob.account())).isEqualByComparingTo("20.00");
    }

    @Test
    @DisplayName("the snapshot shows a locked account, so the caller can refuse it before anything changes")
    void lockedSnapshot() {
        Customer bob = newCustomer("recv_bob");
        assertStatus(patch("/api/accounts/" + bob.account().id() + "/status?status=LOCKED", adminToken()), 200);

        AccountSnapshot account = inTransaction().execute(
                status -> receiveFundsUseCase.lockForReceiving(bob.account().number()));

        assertThat(account.isLocked()).isTrue();
    }

    @Test
    @DisplayName("outside a transaction both methods refuse to run (the lock would be released at once)")
    void requiresTransaction() {
        Customer bob = newCustomer("recv_bob");

        assertThatThrownBy(() -> receiveFundsUseCase.lockForReceiving(bob.account().number()))
                .isInstanceOf(IllegalTransactionStateException.class);
        assertThatThrownBy(() -> receiveFundsUseCase.receive(bob.account().id(), BigDecimal.TEN))
                .isInstanceOf(IllegalTransactionStateException.class);
        assertThat(balanceOf(bob.account())).isEqualByComparingTo("20.00");
    }
}
