package com.Banking_app.transaction.application.service;

import com.Banking_app.transaction.application.port.in.command.RecordCashDepositCommand;
import com.Banking_app.transaction.application.port.in.usecase.RecordCashDepositUseCase;
import com.Banking_app.transaction.application.port.out.SaveTransactionPort;
import com.Banking_app.transaction.application.port.out.TransactionReferencePort;
import com.Banking_app.transaction.domain.models.transaction.Transaction;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Records a top-up: one SUCCESS transaction without a from-account, with one CREDIT ledger entry.
 * Same rows as the old LegacyCashDepositAdapter: "CDM-" reference, and created_at = success_at = the ledger
 * entry's time = the moment of the top-up.
 */
@Service
public class RecordCashDepositService implements RecordCashDepositUseCase {

    private final SaveTransactionPort saveTransactionPort;
    private final TransactionReferencePort transactionReferencePort;

    public RecordCashDepositService(SaveTransactionPort saveTransactionPort,
                                    TransactionReferencePort transactionReferencePort) {
        this.saveTransactionPort = saveTransactionPort;
        this.transactionReferencePort = transactionReferencePort;
    }

    @Override
    @Transactional   // joins the top-up's transaction: balance and history are stored together or not at all
    public String record(RecordCashDepositCommand command) {
        Transaction deposit = Transaction.startCashDeposit(transactionReferencePort.nextCashDeposit(),
                        command.toAccountId(), command.amount(), command.description(), command.idempotencyKey(),
                        command.depositedAt())
                .completeCashDeposit(command.balanceAfter(), command.depositedAt());
        return saveTransactionPort.save(deposit).getReferenceNumber();
    }
}
