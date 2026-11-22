package com.Banking_app.userProfile.application.exception;

/**
 * The current password given with a password change is wrong. 400 on the currentPassword field, not 401: the
 * caller is signed in, and a 401 would make the frontend refresh its token and retry. Counts as a failed login
 * for the brute-force protection (AuthService).
 */
public class IncorrectCurrentPasswordException extends PasswordChangeRejectedException {

    public IncorrectCurrentPasswordException() {
        super("currentPassword", "Current password is incorrect");
    }
}
