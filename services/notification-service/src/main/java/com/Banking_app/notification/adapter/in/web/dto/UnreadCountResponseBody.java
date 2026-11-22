package com.Banking_app.notification.adapter.in.web.dto;

/** GET /api/notifications/unread-count — the number on the header bell. */
public record UnreadCountResponseBody(long unread) {
}
