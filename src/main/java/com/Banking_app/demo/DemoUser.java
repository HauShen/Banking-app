package com.Banking_app.demo;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.UUID;

/**
 * One seeded user. The demo package only knows the table layout, so the role is its own enum here; its names match
 * {@code UserRole} (DemoCastTest checks that), because they are written as text into {@code users.role}.
 *
 * @param username shown on the login page and in the demo tour, so it never changes
 * @param fullName {@code users.full_name}
 * @param email    {@code users.email}, unique like the column
 * @param role     CUSTOMER or ADMIN
 */
public record DemoUser(String username, String fullName, String email, Role role) {

    public enum Role { CUSTOMER, ADMIN }

    public DemoUser {
        Objects.requireNonNull(username, "username");
        Objects.requireNonNull(fullName, "fullName");
        Objects.requireNonNull(email, "email");
        Objects.requireNonNull(role, "role");
    }

    /**
     * {@code users.id}: a UUID made from the username, so a user gets the same id after every reset. Same seed, same
     * data, as the plan promises, including the ids.
     */
    public String id() {
        return UUID.nameUUIDFromBytes(("banking-demo-user:" + username).getBytes(StandardCharsets.UTF_8)).toString();
    }

    public boolean isAdmin() {
        return role == Role.ADMIN;
    }
}
