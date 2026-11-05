package com.Banking_app.exception;

/**
 * Base class for "logged in, but this is not yours" errors thrown by use cases → 403 Forbidden with the
 * exception's message (one handler in GlobalExceptionHandler).
 * Each module has its own subclass, e.g. AccountAccessDeniedException, so modules don't share exceptions.
 */
public abstract class ForbiddenException extends RuntimeException {
    protected ForbiddenException(String message) {
        super(message);
    }
}
