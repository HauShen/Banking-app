package com.Banking_app.notification.exception;

import org.springframework.http.HttpStatus;

import java.time.Instant;

/**
 * The error body, in the same shape as the main app's ApiError, so the frontend reads errors from both the same way
 * (it shows {@code message}).
 */
public record ApiError(Instant timestamp, int status, String error, String message, String path) {

    public static ApiError of(HttpStatus status, String message, String path) {
        return new ApiError(Instant.now(), status.value(), status.getReasonPhrase(), message, path);
    }
}
