package com.Banking_app.account.application.port.out;

/** Does any money movement (transfer or top-up) reference this account? */
public interface AccountHistoryPort {
    boolean hasTransactions(Long accountId);
}
