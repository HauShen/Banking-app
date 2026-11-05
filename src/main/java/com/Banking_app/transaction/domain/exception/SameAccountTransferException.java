package com.Banking_app.transaction.domain.exception;

/**
 * A transfer whose from-account and to-account are the same.
 * Extends IllegalArgumentException so GlobalExceptionHandler keeps answering 400, with the same message as before.
 */
public class SameAccountTransferException extends IllegalArgumentException {
    public SameAccountTransferException() {
        super("Cannot transfer to the same account");
    }
}
