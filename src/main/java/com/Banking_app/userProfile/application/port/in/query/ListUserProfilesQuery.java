package com.Banking_app.userProfile.application.port.in.query;

import java.util.Objects;

public record ListUserProfilesQuery(int page,
                                    int size,
                                    UserSortField sortBy,
                                    SortDirection direction) {

    public static final int DEFAULT_SIZE = 20;
    public static final int MAX_SIZE = 100;

    public ListUserProfilesQuery {
        if (page < 0) {
            throw new IllegalArgumentException("page must not be negative");
        }
        size = size < 1 ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);
        sortBy = Objects.requireNonNullElse(sortBy, UserSortField.USERNAME);
        direction = Objects.requireNonNullElse(direction, SortDirection.ASC);
    }

    /** The defaults an admin gets when they pass no parameters at all. */
    public static ListUserProfilesQuery firstPage() {
        return new ListUserProfilesQuery(0, DEFAULT_SIZE, UserSortField.USERNAME, SortDirection.ASC);
    }
}
