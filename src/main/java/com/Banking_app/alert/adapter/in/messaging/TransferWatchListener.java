package com.Banking_app.alert.adapter.in.messaging;

import com.Banking_app.alert.application.port.in.command.CheckTransferCommand;
import com.Banking_app.alert.application.port.in.usecase.CheckTransferUseCase;
import com.Banking_app.config.kafka.EventEnvelope;
import com.Banking_app.config.kafka.KafkaTopics;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import static com.Banking_app.config.kafka.EventEnvelope.decimal;
import static com.Banking_app.config.kafka.EventEnvelope.text;

/**
 * Inbound messaging adapter: reads the same topic as the notification consumer, but in its own consumer group
 * {@value #GROUP_ID}. Each group has its own offsets, so both consumers get every message, independently: one
 * being slow or down doesn't hold up the other.
 *
 * <p>Only transfers are checked; other event types are skipped. Unreadable messages → dead-letter topic (see
 * EventEnvelope, KafkaConsumerConfig).
 */
@Component
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true")
public class TransferWatchListener {

    public static final String GROUP_ID = "banking-alerts";
    static final int SUPPORTED_VERSION = 1;

    private final CheckTransferUseCase checkTransferUseCase;

    public TransferWatchListener(CheckTransferUseCase checkTransferUseCase) {
        this.checkTransferUseCase = checkTransferUseCase;
    }

    @KafkaListener(topics = KafkaTopics.TRANSACTIONS, groupId = GROUP_ID)
    public void onMessage(ConsumerRecord<String, String> record) {
        EventEnvelope envelope = EventEnvelope.parse(record.value());
        envelope.requireVersion(SUPPORTED_VERSION);
        if (!"TRANSFER_COMPLETED".equals(envelope.eventType())) {
            return;
        }
        JsonNode payload = envelope.payload();
        checkTransferUseCase.check(new CheckTransferCommand(envelope.eventId(), envelope.occurredAt(),
                text(payload, "reference"), text(payload, "fromAccountNumber"), text(payload, "fromOwnerId"),
                text(payload, "toAccountNumber"), decimal(payload, "amount")));
    }
}
