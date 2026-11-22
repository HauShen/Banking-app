package com.Banking_app.notification.application.service;

import com.Banking_app.notification.application.exception.NotificationNotFoundException;
import com.Banking_app.notification.application.port.in.usecase.MarkNotificationsReadUseCase;
import com.Banking_app.notification.application.port.out.LoadNotificationPort;
import com.Banking_app.notification.application.port.out.SaveNotificationPort;
import com.Banking_app.notification.domain.models.Notification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Service
@Transactional
public class MarkNotificationsReadService implements MarkNotificationsReadUseCase {

    private final LoadNotificationPort loadNotificationPort;
    private final SaveNotificationPort saveNotificationPort;
    private final Clock clock;

    public MarkNotificationsReadService(LoadNotificationPort loadNotificationPort,
                                        SaveNotificationPort saveNotificationPort,
                                        Clock clock) {
        this.loadNotificationPort = loadNotificationPort;
        this.saveNotificationPort = saveNotificationPort;
        this.clock = clock;
    }

    @Override
    public void markRead(String userId, long notificationId) {
        Notification notification = loadNotificationPort.findById(notificationId)
                .filter(n -> n.belongsTo(userId))
                .orElseThrow(NotificationNotFoundException::new);
        if (!notification.isRead()) {
            saveNotificationPort.save(notification.markRead(clock.instant()));
        }
    }

    @Override
    public int markAllRead(String userId) {
        return saveNotificationPort.markAllRead(userId, clock.instant());
    }
}
