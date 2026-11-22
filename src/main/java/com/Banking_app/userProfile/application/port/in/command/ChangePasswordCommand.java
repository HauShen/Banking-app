package com.Banking_app.userProfile.application.port.in.command;

/**
 * The signed-in user changes their own password; the current one proves it's really them (a stolen access token
 * alone is not enough to take the account over).
 */
public record ChangePasswordCommand(String userId, String currentPassword, String newPassword) {

    /** Same rule as registration (RegisterUserProfileCommand, UserProfileRequestBody). */
    public static final int PASSWORD_MIN_LENGTH = 8;
    public static final int PASSWORD_MAX_LENGTH = 100;

    public ChangePasswordCommand {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("userId is required");
        }
        if (currentPassword == null || currentPassword.isEmpty()) {
            throw new IllegalArgumentException("currentPassword is required");
        }
        if (newPassword == null || newPassword.length() < PASSWORD_MIN_LENGTH
                || newPassword.length() > PASSWORD_MAX_LENGTH) {
            throw new IllegalArgumentException("newPassword must be " + PASSWORD_MIN_LENGTH + "-"
                    + PASSWORD_MAX_LENGTH + " characters");
        }
    }

    /** Overridden so a stray log line can never print a password. */
    @Override
    public String toString() {
        return "ChangePasswordCommand{userId='" + userId + "'}";
    }
}
