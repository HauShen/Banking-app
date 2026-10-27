package com.Banking_app.account.domain.exception;

/**
 * Amount is null, zero or negative.
 * Extends IllegalArgumentException so the existing GlobalExceptionHandler still returns 400.
 */
public class InvalidAmountException extends IllegalArgumentException {
    public InvalidAmountException(String message) {
        super(message);
    }
}
