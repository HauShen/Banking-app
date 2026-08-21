package com.Banking_app.userProfile.application.port.in.command;

import com.Banking_app.userProfile.domain.enums.UserRole;

import java.util.Objects;

public record ChangeUserRoleCommand(String targetUserId, UserRole newRole, String actingAdminId) {
    public ChangeUserRoleCommand {
        targetUserId = requireText(targetUserId, "targetUserId");
        actingAdminId = requireText(actingAdminId, "actingAdminId");
        Objects.requireNonNull(newRole, "newRole is required");
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }
}
