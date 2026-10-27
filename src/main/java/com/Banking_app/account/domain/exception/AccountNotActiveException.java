package com.Banking_app.account.domain.exception;

/**
 * Operation needs an ACTIVE account.
 * Temporarily extends IllegalArgumentException so the existing GlobalExceptionHandler
 * still returns 400 (behaviour unchanged). Phase 3 can give it its own handler (e.g. 409).
 */
public class AccountNotActiveException extends IllegalArgumentException {
    public AccountNotActiveException(String message) {
        super(message);
    }
}
