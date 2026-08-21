package com.Banking_app.userProfile.application.port.in.command;

public record DeleteUserProfileCommand(String targetUserId, String actingAdminId) {
    public DeleteUserProfileCommand {
        targetUserId = requireText(targetUserId, "targetUserId");
        actingAdminId = requireText(actingAdminId, "actingAdminId");
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }
}
