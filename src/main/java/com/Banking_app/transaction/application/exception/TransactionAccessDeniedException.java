package com.Banking_app.transaction.application.exception;

import com.Banking_app.exception.ForbiddenException;

/**
 * Logged in, but not allowed: transferring from someone else's account, or viewing someone else's
 * transaction or history. 403 through GlobalExceptionHandler.handleForbidden, with the use case's message.
 */
public class TransactionAccessDeniedException extends ForbiddenException {
    public TransactionAccessDeniedException(String message) {
        super(message);
    }
}
