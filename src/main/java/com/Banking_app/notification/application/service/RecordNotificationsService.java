package com.Banking_app.notification.application.service;

import com.Banking_app.notification.application.port.in.command.CashDepositedNotice;
import com.Banking_app.notification.application.port.in.command.RemittanceReceivedNotice;
import com.Banking_app.notification.application.port.in.command.TransferCompletedNotice;
import com.Banking_app.notification.application.port.in.usecase.RecordNotificationsUseCase;
import com.Banking_app.notification.application.port.out.ProcessedEventsPort;
import com.Banking_app.notification.application.port.out.SaveNotificationPort;
import com.Banking_app.notification.domain.models.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

/**
 * Turns events into notifications, exactly once per event even though Kafka may deliver an event more than once:
 * <ol>
 *   <li>mark the event as processed by this consumer (processed_events) — already there → stop, it's a redelivery</li>
 *   <li>store the notifications</li>
 * </ol>
 * Both in one database transaction, so either both happen or neither does.
 */
@Service
public class RecordNotificationsService implements RecordNotificationsUseCase {

    /** This consumer's name in processed_events. Another consumer (e.g. alerts) keeps its own marks. */
    static final String CONSUMER = "notifications";

    private static final Logger log = LoggerFactory.getLogger(RecordNotificationsService.class);

    private final ProcessedEventsPort processedEventsPort;
    private final SaveNotificationPort saveNotificationPort;
    private final Clock clock;

    public RecordNotificationsService(ProcessedEventsPort processedEventsPort,
                                      SaveNotificationPort saveNotificationPort,
                                      Clock clock) {
        this.processedEventsPort = processedEventsPort;
        this.saveNotificationPort = saveNotificationPort;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void transferCompleted(TransferCompletedNotice n) {
        if (alreadyProcessed(n.eventId())) {
            return;
        }
        saveNotificationPort.saveAll(List.of(
                Notification.moneySent(n.eventId(), n.fromOwnerId(), n.reference(), n.amount(), n.original(),
                        n.toAccountNumber(), n.occurredAt()),
                Notification.moneyReceived(n.eventId(), n.toOwnerId(), n.reference(), n.amount(),
                        n.fromAccountNumber(), n.occurredAt())));
    }

    @Override
    @Transactional
    public void cashDeposited(CashDepositedNotice n) {
        if (alreadyProcessed(n.eventId())) {
            return;
        }
        saveNotificationPort.saveAll(List.of(Notification.cashDeposited(n.eventId(), n.ownerId(), n.reference(),
                n.amount(), n.accountNumber(), n.occurredAt())));
    }

    @Override
    @Transactional
    public void remittanceReceived(RemittanceReceivedNotice n) {
        if (alreadyProcessed(n.eventId())) {
            return;
        }
        saveNotificationPort.saveAll(List.of(Notification.remittanceReceived(n.eventId(), n.ownerId(),
                n.reference(), n.amount(), n.original(), n.accountNumber(), n.occurredAt())));
    }

    private boolean alreadyProcessed(UUID eventId) {
        boolean firstTime = processedEventsPort.markProcessed(CONSUMER, eventId, clock.instant());
        if (!firstTime) {
            log.info("Notifications: event {} was already processed, skipping the redelivery", eventId);
        }
        return !firstTime;
    }
}
