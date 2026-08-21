package com.Banking_app.userProfile.application.port.in.command;

import java.util.Objects;

public record CreateAdminCommand(RegisterUserProfileCommand registration, String actingAdminId) {
    public CreateAdminCommand {
        Objects.requireNonNull(registration, "registration is required");
        if (actingAdminId == null || actingAdminId.isBlank()) {
            throw new IllegalArgumentException("actingAdminId is required");
        }
        actingAdminId = actingAdminId.trim();
    }
}
