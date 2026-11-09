package com.Banking_app.exception;

/**
 * Base class for "this request conflicts with data that already exists" errors thrown by use cases → 409 Conflict
 * with the exception's message (one handler in GlobalExceptionHandler). Same idea as ForbiddenException.
 */
public abstract class ConflictException extends RuntimeException {
    protected ConflictException(String message) {
        super(message);
    }
}
