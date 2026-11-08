package com.Banking_app.transaction.domain.exception;

/**
 * Amount missing, zero, negative, or with more than 2 decimals. The request validation (TransferRequestBody) and the
 * account module catch these first, so this is a last line of defence. 400 via IllegalArgumentException.
 */
public class InvalidTransactionAmountException extends IllegalArgumentException {

    /** Missing, zero or negative amount. */
    public InvalidTransactionAmountException() {
        super("Amount must be greater than 0");
    }

    private InvalidTransactionAmountException(String message) {
        super(message);
    }

    public static InvalidTransactionAmountException tooManyDecimals() {
        return new InvalidTransactionAmountException("Amount can have at most 2 decimal places");
    }
}
