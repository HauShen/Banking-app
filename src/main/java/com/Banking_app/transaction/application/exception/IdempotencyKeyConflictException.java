package com.Banking_app.transaction.application.exception;

/**
 * The idempotency key belongs to a transaction from a different account (or to a cash deposit) — also when two
 * accounts send the same key at the same moment and the other one is stored first.
 * 400 via IllegalArgumentException, same message as before.
 */
public class IdempotencyKeyConflictException extends IllegalArgumentException {
    public IdempotencyKeyConflictException() {
        super("idempotencyKey has already been used for a different transfer");
    }
}
