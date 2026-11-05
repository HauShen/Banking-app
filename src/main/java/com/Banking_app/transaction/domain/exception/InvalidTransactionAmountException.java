package com.Banking_app.transaction.domain.exception;

/**
 * Amount missing, zero or negative. Today the request validation (@DecimalMin on TransferRequestBody) and the
 * account module catch this first, so it is a last line of defence. 400 via IllegalArgumentException.
 */
public class InvalidTransactionAmountException extends IllegalArgumentException {
    public InvalidTransactionAmountException() {
        super("Amount must be greater than 0");
    }
}
