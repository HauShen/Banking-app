package com.Banking_app.userProfile.application.exception;

/**
 * A password change the user can fix: 400 with the message on the form field it belongs to
 * ({@code fieldErrors}), through GlobalExceptionHandler.
 */
public class PasswordChangeRejectedException extends RuntimeException {

    private final String field;

    public PasswordChangeRejectedException(String field, String message) {
        super(message);
        this.field = field;
    }

    /** The request field the message belongs to: currentPassword or newPassword. */
    public String getField() {
        return field;
    }

    public static PasswordChangeRejectedException sameAsCurrent() {
        return new PasswordChangeRejectedException("newPassword", "New password must differ from the current one");
    }
}
