package com.Banking_app.notification.application.port.in.usecase;

import com.Banking_app.notification.application.port.in.query.ListNotificationsQuery;
import com.Banking_app.notification.application.port.in.result.NotificationView;
import com.Banking_app.shared.PageResult;

/** A user reads their own notifications. Admins see only their own too: notifications are personal. */
public interface ListNotificationsUseCase {

    PageResult<NotificationView> list(ListNotificationsQuery query);

    long countUnread(String userId);
}
