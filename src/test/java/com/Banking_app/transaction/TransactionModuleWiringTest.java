package com.Banking_app.transaction;

import com.Banking_app.account.application.port.in.usecase.MoveFundsUseCase;
import com.Banking_app.account.domain.exception.InsufficientBalanceException;
import com.Banking_app.shared.PageResult;
import com.Banking_app.shared.Requester;
import com.Banking_app.support.AbstractIntegrationTest;
import com.Banking_app.transaction.application.port.in.command.TransferMoneyCommand;
import com.Banking_app.transaction.application.port.in.query.ListAccountTransactionsQuery;
import com.Banking_app.transaction.application.port.in.result.TransactionView;
import com.Banking_app.transaction.application.port.in.result.TransferResult;
import com.Banking_app.transaction.application.port.in.usecase.AccountHistoryUseCase;
import com.Banking_app.transaction.application.port.in.usecase.ListAccountTransactionsUseCase;
import com.Banking_app.transaction.application.port.in.usecase.TransferMoneyUseCase;
import com.Banking_app.transaction.domain.enums.TransactionStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.IllegalTransactionStateException;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The new transaction module wired by Spring with its real adapters (account module, PostgreSQL), called directly
 * through its use cases — before step 9 points the HTTP endpoints at it. Balances are read through the
 * existing account endpoints.
 */
class TransactionModuleWiringTest extends AbstractIntegrationTest {

    @Autowired
    private TransferMoneyUseCase transferMoneyUseCase;

    @Autowired
    private ListAccountTransactionsUseCase listAccountTransactionsUseCase;

    @Autowired
    private AccountHistoryUseCase accountHistoryUseCase;

    @Autowired
    private MoveFundsUseCase moveFundsUseCase;

    @Autowired
    private JdbcTemplate jdbc;

    private static String key() {
        return "wiring-" + UUID.randomUUID();
    }

    private TransferMoneyCommand command(Customer from, Customer to, String amount, String idempotencyKey) {
        return new TransferMoneyCommand(userIdOf(from.token()), from.account().number(), to.account().number(),
                new BigDecimal(amount), "wiring test", idempotencyKey);
    }

    @Test
    @DisplayName("a transfer through the use case moves the money and writes the transaction + 2 ledger rows")
    void transferThroughUseCase() {
        Customer alice = newCustomer("wire_alice");
        Customer bob = newCustomer("wire_bob");
        topUp(alice.token(), alice.account(), "100.00");             // 20 + 100 = 120
        String key = key();

        TransferResult result = transferMoneyUseCase.transfer(command(alice, bob, "30.00", key));

        assertThat(result.status()).isEqualTo(TransactionStatus.SUCCESS);
        assertThat(result.duplicate()).isFalse();
        assertThat(result.transactionReference()).matches("TXN-[0-9A-F]{12}");
        assertThat(balanceOf(alice.account())).isEqualByComparingTo("90.00");
        assertThat(balanceOf(bob.account())).isEqualByComparingTo("50.00");
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM ledgers l JOIN transactions t ON t.id = l.transaction_id WHERE t.idempotency_key = ?",
                Integer.class, key)).isEqualTo(2);

        // the old endpoint still reads it (the recipient may see it)
        assertStatus(get("/api/transfers/" + result.transactionReference(), bob.token()), 200);
    }

    @Test
    @DisplayName("the same key again returns the original and moves no money")
    void duplicate() {
        Customer alice = newCustomer("wire_alice");
        Customer bob = newCustomer("wire_bob");
        topUp(alice.token(), alice.account(), "100.00");
        String key = key();

        TransferResult first = transferMoneyUseCase.transfer(command(alice, bob, "30.00", key));
        TransferResult again = transferMoneyUseCase.transfer(command(alice, bob, "30.00", key));

        assertThat(again.duplicate()).isTrue();
        assertThat(again.transactionReference()).isEqualTo(first.transactionReference());
        assertThat(balanceOf(alice.account())).isEqualByComparingTo("90.00");
    }

    @Test
    @DisplayName("below the minimum balance: 400 exception and a full rollback — no balance change, no transaction row")
    void rollbackOnInsufficientBalance() {
        Customer alice = newCustomer("wire_alice");
        Customer bob = newCustomer("wire_bob");
        String key = key();

        assertThatThrownBy(() -> transferMoneyUseCase.transfer(command(alice, bob, "0.01", key)))
                .isInstanceOf(InsufficientBalanceException.class)
                .hasMessage("Insufficient balance. Minimum balance of $20.00 must be maintained.");
        assertThat(balanceOf(alice.account())).isEqualByComparingTo("20.00");
        assertThat(balanceOf(bob.account())).isEqualByComparingTo("20.00");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM transactions WHERE idempotency_key = ?",
                Integer.class, key)).isZero();
    }

    @Test
    @DisplayName("the history use case shows the transfer with account numbers (directory adapter wired)")
    void historyThroughUseCase() {
        Customer alice = newCustomer("wire_alice");
        Customer bob = newCustomer("wire_bob");
        topUp(alice.token(), alice.account(), "100.00");
        TransferResult result = transferMoneyUseCase.transfer(command(alice, bob, "30.00", key()));

        PageResult<TransactionView> page = listAccountTransactionsUseCase.list(
                new ListAccountTransactionsQuery(alice.account().number(), 0, 10),
                new Requester(userIdOf(alice.token()), false));

        TransactionView newest = page.content().get(0);
        assertThat(newest.referenceNumber()).isEqualTo(result.transactionReference());
        assertThat(newest.fromAccountNumber()).isEqualTo(alice.account().number());
        assertThat(newest.toAccountNumber()).isEqualTo(bob.account().number());
        assertThat(newest.currency()).isEqualTo("MYR");
        assertThat(page.totalElements()).isEqualTo(2L);                // the top-up + the transfer
        assertThat(page.content().get(1).fromAccountNumber()).isNull(); // the top-up (cash deposit)
    }

    @Test
    @DisplayName("account history: true after a top-up, false for a fresh account")
    void accountHistory() {
        Customer fresh = newCustomer("wire_fresh");
        Customer toppedUp = newCustomer("wire_topped");
        topUp(toppedUp.token(), toppedUp.account(), "10.00");

        assertThat(accountHistoryUseCase.hasTransactions(fresh.account().id())).isFalse();
        assertThat(accountHistoryUseCase.hasTransactions(toppedUp.account().id())).isTrue();
    }

    @Test
    @DisplayName("MoveFundsUseCase refuses to run outside a database transaction (Propagation.MANDATORY)")
    void moveFundsNeedsATransaction() {
        Customer alice = newCustomer("wire_alice");
        Customer bob = newCustomer("wire_bob");

        assertThatThrownBy(() -> moveFundsUseCase.lockPairForTransfer(alice.account().number(), bob.account().number()))
                .isInstanceOf(IllegalTransactionStateException.class);
    }
}
