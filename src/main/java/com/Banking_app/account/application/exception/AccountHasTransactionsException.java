package com.Banking_app.account.application.exception;

/**
 * The account has transaction history, so deleting it would break that history.
 * GlobalExceptionHandler maps it to 409 Conflict.
 */
public class AccountHasTransactionsException extends RuntimeException {
    public AccountHasTransactionsException(Long accountId) {
        super("Account " + accountId + " has transactions and cannot be deleted");
    }
}
