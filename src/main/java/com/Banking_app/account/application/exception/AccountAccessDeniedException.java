package com.Banking_app.account.application.exception;
import com.Banking_app.exception.ForbiddenException;

/**
 * Caller is logged in but is neither the owner of the account nor an admin.
 * GlobalExceptionHandler maps it to 403 Forbidden.
 */
public class AccountAccessDeniedException extends ForbiddenException {
    public AccountAccessDeniedException(String message) {
        super(message);
    }
}
