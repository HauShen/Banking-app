package com.Banking_app.account.application.port.in;

import java.util.Objects;

/**
 * Who is calling a use case. Built by the web adapter from the logged-in user,
 * so the application layer can enforce "owner or admin" without knowing Spring Security.
 */
public record AccountRequester(String userId, boolean admin) {

    public AccountRequester {
        Objects.requireNonNull(userId, "userId is required");
    }

    /** Admins can see every user's accounts; customers only their own. */
    public boolean canAccessAccountsOf(String ownerId) {
        return admin || userId.equals(ownerId);
    }
}
