package com.Banking_app.notification.application.service;

import com.Banking_app.notification.application.port.in.query.ListNotificationsQuery;
import com.Banking_app.notification.application.port.in.result.NotificationView;
import com.Banking_app.notification.application.port.in.usecase.ListNotificationsUseCase;
import com.Banking_app.notification.application.port.out.LoadNotificationPort;
import com.Banking_app.shared.PageResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ListNotificationsService implements ListNotificationsUseCase {

    private final LoadNotificationPort loadNotificationPort;

    public ListNotificationsService(LoadNotificationPort loadNotificationPort) {
        this.loadNotificationPort = loadNotificationPort;
    }

    @Override
    public PageResult<NotificationView> list(ListNotificationsQuery query) {
        return loadNotificationPort.findPage(query.userId(), query.unreadOnly(), query.page(), query.size())
                .map(NotificationView::of);
    }

    @Override
    public long countUnread(String userId) {
        return loadNotificationPort.countUnread(userId);
    }
}
