package com.Banking_app.transaction.application.port.in.usecase;

import com.Banking_app.transaction.application.port.in.command.RecordForeignRemittanceCommand;
import com.Banking_app.transaction.application.port.in.result.RemittanceResult;

/**
 * Credits money from abroad: converted to MYR at the bank's buy rate, recorded as a FOREIGN_REMITTANCE (no
 * from-account, one CREDIT ledger entry). Who may call it is decided by the web adapter (admins only: customers can't
 * create incoming money themselves).
 */
public interface RecordForeignRemittanceUseCase {

    /**
     * @throws com.Banking_app.account.application.exception.TransferAccountNotFoundException  404 unknown account
     * @throws com.Banking_app.transaction.application.exception.IdempotencyKeyMismatchException 409 key used for
     *                                                                                          something else
     * @throws com.Banking_app.transaction.application.exception.TransferAccountLockedException   400 locked account
     * @throws IllegalArgumentException 400 from the fx module: MYR, currency not offered, too many decimals
     */
    RemittanceResult receive(RecordForeignRemittanceCommand command);
}
