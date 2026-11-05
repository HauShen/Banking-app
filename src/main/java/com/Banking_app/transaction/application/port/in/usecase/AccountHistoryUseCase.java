package com.Banking_app.transaction.application.port.in.usecase;

/**
 * INTERNAL use case, not exposed over HTTP: the account module asks it before deleting an account
 * (an account with history can't be deleted → 409).
 */
public interface AccountHistoryUseCase {
    /** True if the account sent or received money in any transaction, cash deposits included. */
    boolean hasTransactions(Long accountId);
}
