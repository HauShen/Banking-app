package com.Banking_app.notification.application.port.in.usecase;

public interface MarkNotificationsReadUseCase {

    /** Unknown or someone else's → 404. Already read → nothing changes. */
    void markRead(String userId, long notificationId);

    /** Returns how many were unread. */
    int markAllRead(String userId);
}
