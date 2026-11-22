package com.Banking_app.alert.application.port.in.query;

import com.Banking_app.alert.domain.enums.AlertStatus;

/** One page of alerts, newest first; {@code status} null = all. Bad paging → 400. */
public record ListAlertsQuery(AlertStatus status, int page, int size) {

    public static final int MAX_SIZE = 100;

    public ListAlertsQuery {
        if (page < 0) {
            throw new IllegalArgumentException("page must not be negative");
        }
        if (size < 1 || size > MAX_SIZE) {
            throw new IllegalArgumentException("size must be between 1 and " + MAX_SIZE);
        }
    }
}
