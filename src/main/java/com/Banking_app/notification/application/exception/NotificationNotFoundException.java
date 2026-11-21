package com.Banking_app.notification.application.exception;

import com.Banking_app.exception.ResourceNotFoundException;

/** Unknown notification, or someone else's: the same 404, so ids of other users' notifications reveal nothing. */
public class NotificationNotFoundException extends ResourceNotFoundException {
    public NotificationNotFoundException() {
        super("Notification not found");
    }
}
