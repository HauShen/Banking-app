package com.Banking_app.notification.shared;

import java.util.Objects;

/**
 * Who is calling a use case: the user id and role from the access token. Built by the web adapter, so the
 * application layer never sees Spring Security.
 */
public record Requester(String userId, boolean admin) {

    public Requester {
        Objects.requireNonNull(userId, "userId is required");
    }
}
