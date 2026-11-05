package com.Banking_app.transaction.application.exception;

import com.Banking_app.exception.ResourceNotFoundException;

/** Unknown transaction reference. 404, same message as before. */
public class TransactionNotFoundException extends ResourceNotFoundException {
    public TransactionNotFoundException() {
        super("Transaction not found");
    }
}
