package com.Banking_app.alert.domain.enums;

/** Which rule raised an alert. Stored as text in transaction_alerts.rule. */
public enum AlertRule {
    /** One transfer at or above the configured amount. */
    LARGE_TRANSFER,
    /** More outgoing transfers from one account within a short time than allowed. */
    RAPID_TRANSFERS
}
