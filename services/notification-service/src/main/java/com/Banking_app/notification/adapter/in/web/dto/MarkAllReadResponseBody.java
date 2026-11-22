package com.Banking_app.notification.adapter.in.web.dto;

/** POST /api/notifications/read-all — how many were unread. */
public record MarkAllReadResponseBody(int marked) {
}
