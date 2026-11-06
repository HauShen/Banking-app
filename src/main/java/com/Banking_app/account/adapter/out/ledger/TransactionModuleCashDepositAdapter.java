package com.Banking_app.account.adapter.out.ledger;

import com.Banking_app.account.application.port.out.RecordCashDepositPort;
import com.Banking_app.transaction.application.port.in.command.RecordCashDepositCommand;
import com.Banking_app.transaction.application.port.in.usecase.RecordCashDepositUseCase;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Outbound adapter: records a top-up through the transaction module's inbound RecordCashDepositUseCase
 * (replaces LegacyCashDepositAdapter, which wrote the rows with the old repositories).
 * Same description and idempotency-key format as before; the transaction module adds the "CDM-" reference.
 * Runs inside TopUpAccountService's transaction, so the balance and the history are saved together.
 */
@Component
public class TransactionModuleCashDepositAdapter implements RecordCashDepositPort {

    static final String DESCRIPTION = "Cash Deposit Machine top-up";

    private final RecordCashDepositUseCase recordCashDepositUseCase;

    public TransactionModuleCashDepositAdapter(RecordCashDepositUseCase recordCashDepositUseCase) {
        this.recordCashDepositUseCase = recordCashDepositUseCase;
    }

    @Override
    public void recordCashDeposit(Long accountId, BigDecimal amount, BigDecimal balanceAfter, Instant depositedAt) {
        recordCashDepositUseCase.record(new RecordCashDepositCommand(accountId, amount, balanceAfter, depositedAt,
                DESCRIPTION, "CDM-" + UUID.randomUUID()));   // a top-up has no client key: unique per top-up
    }
}
