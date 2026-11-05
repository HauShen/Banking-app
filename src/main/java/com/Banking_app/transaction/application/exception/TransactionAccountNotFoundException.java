package com.Banking_app.transaction.application.exception;

import com.Banking_app.exception.ResourceNotFoundException;

/** The account whose history was asked for does not exist. 404, same message as before. */
public class TransactionAccountNotFoundException extends ResourceNotFoundException {
    public TransactionAccountNotFoundException(String accountNumber) {
        super("Account not found: " + accountNumber);
    }
}
