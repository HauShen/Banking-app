package com.Banking_app.transaction.application.port.out;

/**
 * Thrown by SaveTransactionPort when another transaction with the same idempotency key was stored first
 * (the database's unique constraint). The use case decides what that means for the caller.
 */
public class DuplicateIdempotencyKeyException extends RuntimeException {
    public DuplicateIdempotencyKeyException(String idempotencyKey, Throwable cause) {
        super("A transaction with idempotency key '" + idempotencyKey + "' already exists", cause);
    }
}
