package com.Banking_app.security;

import java.time.Duration;

/** Login refused before the password is even checked: the username or the client IP is temporarily locked. -> 429 */
public class TooManyLoginAttemptsException extends RuntimeException {

    private final Duration retryAfter;

    public TooManyLoginAttemptsException(Duration retryAfter) {
        super("Too many failed login attempts. Try again later.");
        this.retryAfter = retryAfter;
    }

    /** Whole seconds for the Retry-After header, rounded up, at least 1. */
    public long getRetryAfterSeconds() {
        return Math.max(1, (retryAfter.toMillis() + 999) / 1000);
    }
}
