package com.Banking_app.notification.application.port.in.result;

import com.Banking_app.notification.domain.enums.NotificationType;
import com.Banking_app.notification.domain.models.Notification;

import java.math.BigDecimal;
import java.time.Instant;

/** A notification as the web adapter shows it. */
public record NotificationView(long id,
                               NotificationType type,
                               String message,
                               String reference,
                               BigDecimal amount,
                               Instant createdAt,
                               boolean read) {

    public static NotificationView of(Notification notification) {
        return new NotificationView(notification.getId(), notification.getType(), notification.getMessage(),
                notification.getReference(), notification.getAmount(), notification.getCreatedAt(),
                notification.isRead());
    }
}
