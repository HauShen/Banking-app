package com.Banking_app.account.adapter.out.ledger;

import com.Banking_app.transaction.application.port.in.command.RecordCashDepositCommand;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** The two account → transaction adapters, with the transaction module's use cases faked by lambdas. */
class TransactionModuleLedgerAdaptersTest {

    @Test
    @DisplayName("top-up: passes account, amounts and time on, with the old description and a CDM- key")
    void cashDeposit() {
        List<RecordCashDepositCommand> received = new ArrayList<>();
        TransactionModuleCashDepositAdapter adapter = new TransactionModuleCashDepositAdapter(command -> {
            received.add(command);
            return "CDM-000000000001";
        });
        Instant at = Instant.parse("2026-11-06T08:00:00Z");

        adapter.recordCashDeposit(7L, new BigDecimal("50.00"), new BigDecimal("70.00"), at);
        adapter.recordCashDeposit(7L, new BigDecimal("50.00"), new BigDecimal("120.00"), at);

        RecordCashDepositCommand command = received.get(0);
        assertThat(command.toAccountId()).isEqualTo(7L);
        assertThat(command.amount()).isEqualByComparingTo("50.00");
        assertThat(command.balanceAfter()).isEqualByComparingTo("70.00");
        assertThat(command.depositedAt()).isEqualTo(at);
        assertThat(command.description()).isEqualTo("Cash Deposit Machine top-up");
        assertThat(command.idempotencyKey()).startsWith("CDM-");
        assertThat(received.get(1).idempotencyKey()).isNotEqualTo(command.idempotencyKey());   // unique per top-up
    }

    @Test
    @DisplayName("history: asks the transaction module")
    void history() {
        TransactionModuleHistoryAdapter adapter = new TransactionModuleHistoryAdapter(accountId -> accountId == 1L);

        assertThat(adapter.hasTransactions(1L)).isTrue();
        assertThat(adapter.hasTransactions(2L)).isFalse();
    }
}
