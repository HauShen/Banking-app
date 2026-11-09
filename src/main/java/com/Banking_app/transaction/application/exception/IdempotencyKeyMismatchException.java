package com.Banking_app.transaction.application.exception;

import com.Banking_app.exception.ConflictException;

/**
 * The account reused its own idempotency key for a DIFFERENT transfer (other amount or recipient). Returning the
 * original transfer would hide the client's mistake, so → 409 Conflict instead.
 */
public class IdempotencyKeyMismatchException extends ConflictException {
    public IdempotencyKeyMismatchException() {
        super("idempotencyKey has already been used for a transfer with a different amount or recipient");
    }
}
