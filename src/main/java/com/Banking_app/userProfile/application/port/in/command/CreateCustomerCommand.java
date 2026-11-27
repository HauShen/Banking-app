package com.Banking_app.userProfile.application.port.in.command;

import java.util.Objects;

/**
 * An admin creates a customer profile (like a bank opening an account at the branch: staff verify the person, the
 * customer doesn't sign themselves up).
 *
 * @param registration  the new customer's details and the initial password the admin hands over
 * @param actingAdminId the admin doing it; recorded in the audit log
 */
public record CreateCustomerCommand(RegisterUserProfileCommand registration, String actingAdminId) {

    public CreateCustomerCommand {
        Objects.requireNonNull(registration, "registration is required");
        if (actingAdminId == null || actingAdminId.isBlank()) {
            throw new IllegalArgumentException("actingAdminId is required");
        }
        actingAdminId = actingAdminId.trim();
    }
}
