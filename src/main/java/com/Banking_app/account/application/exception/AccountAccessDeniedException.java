package com.Banking_app.account.application.exception;

/**
 * Caller is not the owner of the account.
 * Temporarily extends IllegalArgumentException so the existing GlobalExceptionHandler
 * still returns 400 (behaviour unchanged). Phase 3, step 13 maps it to 403.
 */
public class AccountAccessDeniedException extends IllegalArgumentException {
    public AccountAccessDeniedException(String message) {
        super(message);
    }
}
