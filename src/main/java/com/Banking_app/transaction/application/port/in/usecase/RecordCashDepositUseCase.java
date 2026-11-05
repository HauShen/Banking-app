package com.Banking_app.transaction.application.port.in.usecase;

import com.Banking_app.transaction.application.port.in.command.RecordCashDepositCommand;

/**
 * INTERNAL use case, not exposed over HTTP: called by the account module's top-up (through its
 * RecordCashDepositPort adapter) inside the top-up's database transaction.
 */
public interface RecordCashDepositUseCase {
    /** @return the new transaction's reference number */
    String record(RecordCashDepositCommand command);
}
