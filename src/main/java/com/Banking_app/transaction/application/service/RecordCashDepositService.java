package com.Banking_app.transaction.application.service;

import com.Banking_app.transaction.application.port.in.command.RecordCashDepositCommand;
import com.Banking_app.transaction.application.port.in.usecase.RecordCashDepositUseCase;
import com.Banking_app.transaction.application.port.out.SaveTransactionPort;
import com.Banking_app.transaction.application.port.out.TransactionReferencePort;
import com.Banking_app.transaction.domain.models.transaction.Transaction;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

/** Records a top-up: one SUCCESS transaction without a from-account, with one CREDIT ledger entry. */
// @Service is added in step 8.
public class RecordCashDepositService implements RecordCashDepositUseCase {

    private final SaveTransactionPort saveTransactionPort;
    private final TransactionReferencePort transactionReferencePort;
    private final Clock clock;

    public RecordCashDepositService(SaveTransactionPort saveTransactionPort,
                                    TransactionReferencePort transactionReferencePort,
                                    Clock clock) {
        this.saveTransactionPort = saveTransactionPort;
        this.transactionReferencePort = transactionReferencePort;
        this.clock = clock;
    }

    @Override
    @Transactional   // joins the top-up's transaction: balance and history are stored together or not at all
    public String record(RecordCashDepositCommand command) {
        Instant now = clock.instant();
        Transaction deposit = Transaction.startCashDeposit(transactionReferencePort.next(), command.toAccountId(),
                        command.amount(), command.description(), command.idempotencyKey(), now)
                .completeCashDeposit(command.balanceAfter(), now);
        return saveTransactionPort.save(deposit).getReferenceNumber();
    }
}
