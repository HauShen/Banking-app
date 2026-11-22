package com.Banking_app.notification.application.port.out;

import com.Banking_app.notification.domain.models.Notification;

import java.time.Instant;
import java.util.List;

public interface SaveNotificationPort {

    void saveAll(List<Notification> notifications);

    /** Updates a stored notification (its read time). */
    void save(Notification notification);

    /** Marks every unread notification of the user as read at {@code at}; returns how many. */
    int markAllRead(String userId, Instant at);
}
