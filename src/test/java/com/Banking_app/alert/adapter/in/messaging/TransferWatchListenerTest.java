package com.Banking_app.alert.adapter.in.messaging;

import com.Banking_app.alert.application.port.in.command.CheckTransferCommand;
import com.Banking_app.config.kafka.UnprocessableMessageException;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TransferWatchListenerTest {

    private static final UUID EVENT_ID = UUID.fromString("4e1d2c3b-0000-4000-8000-000000000002");

    private final List<CheckTransferCommand> checked = new ArrayList<>();
    private final TransferWatchListener listener = new TransferWatchListener(checked::add);

    private void receive(String eventType, int version, String payload) {
        String value = """
                {"eventId":"%s","eventType":"%s","version":%d,"occurredAt":"2026-11-21T08:00:00Z","payload":%s}"""
                .formatted(EVENT_ID, eventType, version, payload);
        listener.onMessage(new ConsumerRecord<>("banking.transactions.v1", 0, 0L, "1000000001", value));
    }

    @Test
    @DisplayName("TRANSFER_COMPLETED → checked, with the sender, the recipient and the exact MYR amount")
    void transfer() {
        receive("TRANSFER_COMPLETED", 1, """
                {"reference":"TXN-1","fromAccountNumber":"1000000001","fromOwnerId":"alice",
                 "toAccountNumber":"1000000002","toOwnerId":"bob","amount":12000.50,"original":null}""");

        assertThat(checked).singleElement().satisfies(c -> {
            assertThat(c.eventId()).isEqualTo(EVENT_ID);
            assertThat(c.occurredAt()).isEqualTo(Instant.parse("2026-11-21T08:00:00Z"));
            assertThat(c.reference()).isEqualTo("TXN-1");
            assertThat(c.fromAccountNumber()).isEqualTo("1000000001");
            assertThat(c.fromOwnerId()).isEqualTo("alice");
            assertThat(c.toAccountNumber()).isEqualTo("1000000002");
            assertThat(c.amount()).isEqualByComparingTo("12000.50");
        });
    }

    @Test
    @DisplayName("deposits and remittances aren't checked: skipped, no error")
    void otherTypes() {
        receive("CASH_DEPOSITED", 1, "{\"reference\":\"CDM-1\",\"accountNumber\":\"1\",\"ownerId\":\"bob\",\"amount\":50}");
        receive("FOREIGN_REMITTANCE_RECEIVED", 1, "{}");

        assertThat(checked).isEmpty();
    }

    @Test
    @DisplayName("an unknown version or a missing field → UnprocessableMessageException (→ dead-letter topic)")
    void unprocessable() {
        assertThatThrownBy(() -> receive("TRANSFER_COMPLETED", 2, "{}"))
                .isInstanceOf(UnprocessableMessageException.class);
        assertThatThrownBy(() -> receive("TRANSFER_COMPLETED", 1, "{\"reference\":\"TXN-1\"}"))
                .isInstanceOf(UnprocessableMessageException.class)
                .hasMessage("Missing field 'fromAccountNumber'");
        assertThat(checked).isEmpty();
    }
}
