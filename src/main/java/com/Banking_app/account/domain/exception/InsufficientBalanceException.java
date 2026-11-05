package com.Banking_app.account.domain.exception;

/**
 * A withdrawal would take the balance below Account.MINIMUM_BALANCE.
 * Extends IllegalArgumentException like the other account domain exceptions, so the API keeps answering 400.
 */
public class InsufficientBalanceException extends IllegalArgumentException {
    public InsufficientBalanceException(String message) {
        super(message);
    }
}
