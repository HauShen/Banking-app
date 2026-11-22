package com.Banking_app.notification.security;

/** The logged-in user, as far as this service knows them: what the access token says. */
public record AuthenticatedUser(String userId, String username) {
}
