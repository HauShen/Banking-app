package com.Banking_app.notification.application.port.in.query;

import java.util.Objects;

/** One page of a user's notifications, newest first. Bad paging → 400. */
public record ListNotificationsQuery(String userId, boolean unreadOnly, int page, int size) {

    public static final int MAX_SIZE = 100;

    public ListNotificationsQuery {
        Objects.requireNonNull(userId, "userId");
        if (page < 0) {
            throw new IllegalArgumentException("page must not be negative");
        }
        if (size < 1 || size > MAX_SIZE) {
            throw new IllegalArgumentException("size must be between 1 and " + MAX_SIZE);
        }
    }
}
