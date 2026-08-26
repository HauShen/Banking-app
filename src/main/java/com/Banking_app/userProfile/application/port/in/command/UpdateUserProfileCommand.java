package com.Banking_app.userProfile.application.port.in.command;

public record UpdateUserProfileCommand(String targetUserId,
                                       String fullName,
                                       String email,
                                       String actingUserId) {

    private static final int FULL_NAME_MAX_LENGTH = 100;

    public UpdateUserProfileCommand {
        targetUserId = requireText(targetUserId, "targetUserId");
        actingUserId = requireText(actingUserId, "actingUserId");
        fullName = requireText(fullName, "fullName");
        // Lowercased for the same reason as registration: the uniqueness check and the value the
        // domain stores must agree, or "HAU@x.com" slips past a check against "hau@x.com".
        email = requireText(email, "email").toLowerCase();

        if (fullName.length() > FULL_NAME_MAX_LENGTH) {
            throw new IllegalArgumentException(
                    "fullName must be at most " + FULL_NAME_MAX_LENGTH + " characters");
        }
    }

    /** True when the user is editing their own profile. */
    public boolean isSelfEdit() {
        return targetUserId.equals(actingUserId);
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }
}
