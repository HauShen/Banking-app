package com.Banking_app.notification.application.port.in.usecase;

import com.Banking_app.notification.application.port.in.command.CashDepositedNotice;
import com.Banking_app.notification.application.port.in.command.RemittanceReceivedNotice;
import com.Banking_app.notification.application.port.in.command.TransferCompletedNotice;

/**
 * Creates the notifications for one event. Safe to call again with the same event (Kafka delivers at least once):
 * the second call changes nothing.
 */
public interface RecordNotificationsUseCase {

    /** Two notifications: "sent" for the sender, "received" for the recipient. */
    void transferCompleted(TransferCompletedNotice notice);

    void cashDeposited(CashDepositedNotice notice);

    void remittanceReceived(RemittanceReceivedNotice notice);
}
