package com.Banking_app.notification.adapter.in.messaging;

import com.Banking_app.notification.application.port.in.command.CashDepositedNotice;
import com.Banking_app.notification.application.port.in.command.RemittanceReceivedNotice;
import com.Banking_app.notification.application.port.in.command.TransferCompletedNotice;
import com.Banking_app.notification.application.port.in.usecase.RecordNotificationsUseCase;
import com.Banking_app.notification.config.kafka.EventEnvelope;
import com.Banking_app.notification.config.kafka.KafkaTopics;
import com.Banking_app.notification.config.kafka.UnprocessableMessageException;
import com.Banking_app.notification.domain.models.ForeignAmount;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import static com.Banking_app.notification.config.kafka.EventEnvelope.decimal;
import static com.Banking_app.notification.config.kafka.EventEnvelope.optional;
import static com.Banking_app.notification.config.kafka.EventEnvelope.text;

/**
 * Inbound messaging adapter: reads the transaction events from Kafka and calls the notification use case.
 *
 * <p>It knows only the message format (the {@link EventEnvelope} and the payload fields), never the main app's
 * classes: the topic is the contract between the main app and this service.
 *
 * <ul>
 *   <li>Unknown event types are skipped: a producer may add new events before this consumer cares about them.</li>
 *   <li>A message that can't be read (not JSON, missing field, unknown version) →
 *       {@link UnprocessableMessageException} → straight to the dead-letter topic.</li>
 *   <li>Anything else that fails (e.g. the database) is retried by the error handler, then dead-lettered.</li>
 * </ul>
 * Consumer group {@value #GROUP_ID}: its offsets on the broker say how far this service has read. Other consumers
 * (e.g. the main app's alerts) use their own group and read the same messages independently.
 *
 * <p>The group is new: the main app's notification module read the topic as "banking-notifications". A group without
 * offsets starts at the oldest message still on the topic (auto-offset-reset=earliest), so on its first start this
 * service rebuilds the notifications from every event Kafka still keeps (7 days).
 */
@Component
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true")
public class TransactionEventsListener {

    public static final String GROUP_ID = "notification-service";
    static final int SUPPORTED_VERSION = 1;

    private static final Logger log = LoggerFactory.getLogger(TransactionEventsListener.class);

    private final RecordNotificationsUseCase recordNotificationsUseCase;

    public TransactionEventsListener(RecordNotificationsUseCase recordNotificationsUseCase) {
        this.recordNotificationsUseCase = recordNotificationsUseCase;
    }

    @KafkaListener(topics = KafkaTopics.TRANSACTIONS, groupId = GROUP_ID)
    public void onMessage(ConsumerRecord<String, String> record) {
        EventEnvelope envelope = EventEnvelope.parse(record.value());
        envelope.requireVersion(SUPPORTED_VERSION);
        JsonNode payload = envelope.payload();

        switch (envelope.eventType()) {
            case "TRANSFER_COMPLETED" -> recordNotificationsUseCase.transferCompleted(new TransferCompletedNotice(
                    envelope.eventId(), envelope.occurredAt(), text(payload, "reference"),
                    text(payload, "fromAccountNumber"), text(payload, "fromOwnerId"),
                    text(payload, "toAccountNumber"), text(payload, "toOwnerId"),
                    decimal(payload, "amount"), foreignAmount(payload)));
            case "CASH_DEPOSITED" -> recordNotificationsUseCase.cashDeposited(new CashDepositedNotice(
                    envelope.eventId(), envelope.occurredAt(), text(payload, "reference"),
                    text(payload, "accountNumber"), text(payload, "ownerId"), decimal(payload, "amount")));
            case "FOREIGN_REMITTANCE_RECEIVED" -> recordNotificationsUseCase.remittanceReceived(
                    new RemittanceReceivedNotice(envelope.eventId(), envelope.occurredAt(), text(payload, "reference"),
                            text(payload, "accountNumber"), text(payload, "ownerId"), decimal(payload, "amount"),
                            foreignAmount(payload)));
            default -> log.debug("Notifications: no notification for event type {}, skipped", envelope.eventType());
        }
    }

    /** {"currency":"USD","amount":20.00,…} or null / absent for a MYR transaction. */
    private static ForeignAmount foreignAmount(JsonNode payload) {
        JsonNode original = optional(payload, "original");
        return original == null ? null : new ForeignAmount(text(original, "currency"), decimal(original, "amount"));
    }
}
