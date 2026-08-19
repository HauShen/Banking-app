package com.Banking_app.userProfile.application.port.in.command;
import java.util.Objects;

public record RegisterUserProfileCommand(String username, String fullName, String email, String rawPassword) {

    private static final int PASSWORD_MIN_LENGTH = 8;

    public RegisterUserProfileCommand {
        username = requireText(username, "username");
        fullName = requireText(fullName, "fullName");

        // Lowercased here so the uniqueness check and the value the domain stores agree.

        email = requireText(email, "email").toLowerCase();
        Objects.requireNonNull(rawPassword, "rawPassword is required");
        if (rawPassword.length() < PASSWORD_MIN_LENGTH) {
            throw new IllegalArgumentException(
                    "password must be at least " + PASSWORD_MIN_LENGTH + " characters");
        }
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }

    /** Overridden so a stray log line can never print the raw password. */
    @Override
    public String toString() {
        return "RegisterUserProfileCommand{username='" + username + "', email='" + email + "'}";
    }

}
