package com.Banking_app.account.application.exception;

/**
 * Caller is logged in but is neither the owner of the account nor an admin.
 * GlobalExceptionHandler maps it to 403 Forbidden.
 */
public class AccountAccessDeniedException extends RuntimeException {
    public AccountAccessDeniedException(String message) {
        super(message);
    }
}
