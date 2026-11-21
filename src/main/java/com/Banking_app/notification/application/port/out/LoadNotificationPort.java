package com.Banking_app.notification.application.port.out;

import com.Banking_app.notification.domain.models.Notification;
import com.Banking_app.shared.PageResult;

import java.util.Optional;

public interface LoadNotificationPort {

    /** Newest first (when the money moved), then by id. */
    PageResult<Notification> findPage(String userId, boolean unreadOnly, int page, int size);

    long countUnread(String userId);

    Optional<Notification> findById(long id);
}
