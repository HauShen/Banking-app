package com.Banking_app.notification.domain.enums;

/** What a notification is about. Stored as text in notifications.type. */
public enum NotificationType {
    /** The user's account sent a transfer. */
    MONEY_SENT,
    /** The user's account received a transfer. */
    MONEY_RECEIVED,
    /** A Cash Deposit Machine top-up was credited to the user's account. */
    CASH_DEPOSITED,
    /** Money from abroad was credited to the user's account. */
    REMITTANCE_RECEIVED
}
