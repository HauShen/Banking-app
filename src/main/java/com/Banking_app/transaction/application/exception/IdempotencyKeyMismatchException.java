package com.Banking_app.transaction.application.exception;

import com.Banking_app.exception.ConflictException;

/**
 * The idempotency key was already used for a DIFFERENT request. Returning the original would hide the client's
 * mistake, so → 409 Conflict instead.
 * <ul>
 *   <li>transfers: the account reused its own key for another amount or recipient</li>
 *   <li>remittances: the gateway reused a key for another account, currency or amount, or the key belongs to a
 *       different kind of transaction (admins see every transaction, so nothing is revealed)</li>
 * </ul>
 */
public class IdempotencyKeyMismatchException extends ConflictException {

    public IdempotencyKeyMismatchException() {
        this("idempotencyKey has already been used for a transfer with a different amount or recipient");
    }

    private IdempotencyKeyMismatchException(String message) {
        super(message);
    }

    public static IdempotencyKeyMismatchException forRemittance() {
        return new IdempotencyKeyMismatchException("idempotencyKey has already been used for a different transaction");
    }
}
