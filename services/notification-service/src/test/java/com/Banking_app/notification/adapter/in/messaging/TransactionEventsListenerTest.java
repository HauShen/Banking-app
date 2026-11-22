package com.Banking_app.notification.adapter.in.messaging;

import com.Banking_app.notification.application.port.in.command.CashDepositedNotice;
import com.Banking_app.notification.application.port.in.command.RemittanceReceivedNotice;
import com.Banking_app.notification.application.port.in.command.TransferCompletedNotice;
import com.Banking_app.notification.application.port.in.usecase.RecordNotificationsUseCase;
import com.Banking_app.notification.config.kafka.UnprocessableMessageException;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Reading the message format, with a fake use case: no Spring, no Kafka. */
class TransactionEventsListenerTest {

    private static final UUID EVENT_ID = UUID.fromString("4e1d2c3b-0000-4000-8000-000000000001");

    private final List<Object> calls = new ArrayList<>();
    private final TransactionEventsListener listener = new TransactionEventsListener(new RecordNotificationsUseCase() {
        @Override
        public void transferCompleted(TransferCompletedNotice notice) {
            calls.add(notice);
        }

        @Override
        public void cashDeposited(CashDepositedNotice notice) {
            calls.add(notice);
        }

        @Override
        public void remittanceReceived(RemittanceReceivedNotice notice) {
            calls.add(notice);
        }
    });

    private static String envelope(String eventType, int version, String payload) {
        return """
                {"eventId":"%s","eventType":"%s","version":%d,"occurredAt":"2026-11-21T08:00:00.123456Z","payload":%s}"""
                .formatted(EVENT_ID, eventType, version, payload);
    }

    private void receive(String value) {
        listener.onMessage(new ConsumerRecord<>("banking.transactions.v1", 0, 0L, "1000000001", value));
    }

    @Test
    @DisplayName("TRANSFER_COMPLETED → transferCompleted with every field; the amount keeps its exact digits")
    void transfer() {
        receive(envelope("TRANSFER_COMPLETED", 1, """
                {"reference":"TXN-1","fromAccountNumber":"1000000001","fromOwnerId":"alice",
                 "toAccountNumber":"1000000002","toOwnerId":"bob","amount":12345678901234567.89,"original":null}"""));

        assertThat(calls).singleElement().isInstanceOfSatisfying(TransferCompletedNotice.class, n -> {
            assertThat(n.eventId()).isEqualTo(EVENT_ID);
            assertThat(n.occurredAt()).isEqualTo(Instant.parse("2026-11-21T08:00:00.123456Z"));
            assertThat(n.reference()).isEqualTo("TXN-1");
            assertThat(n.fromAccountNumber()).isEqualTo("1000000001");
            assertThat(n.fromOwnerId()).isEqualTo("alice");
            assertThat(n.toAccountNumber()).isEqualTo("1000000002");
            assertThat(n.toOwnerId()).isEqualTo("bob");
            assertThat(n.amount()).isEqualByComparingTo(new BigDecimal("12345678901234567.89"));   // a double can't
            assertThat(n.original()).isNull();
        });
    }

    @Test
    @DisplayName("a foreign amount is read into ForeignAmount (the rate isn't needed for a notification)")
    void foreignTransfer() {
        receive(envelope("TRANSFER_COMPLETED", 1, """
                {"reference":"TXN-1","fromAccountNumber":"1000000001","fromOwnerId":"alice",
                 "toAccountNumber":"1000000002","toOwnerId":"bob","amount":95.00,
                 "original":{"currency":"USD","amount":20.00,"exchangeRate":4.75}}"""));

        TransferCompletedNotice n = (TransferCompletedNotice) calls.get(0);
        assertThat(n.original().currency()).isEqualTo("USD");
        assertThat(n.original().amount()).isEqualByComparingTo("20.00");
    }

    @Test
    @DisplayName("CASH_DEPOSITED and FOREIGN_REMITTANCE_RECEIVED → their own use-case methods")
    void depositAndRemittance() {
        receive(envelope("CASH_DEPOSITED", 1, """
                {"reference":"CDM-1","accountNumber":"1000000002","ownerId":"bob","amount":50.00}"""));
        receive(envelope("FOREIGN_REMITTANCE_RECEIVED", 1, """
                {"reference":"RMT-1","accountNumber":"1000000002","ownerId":"bob","amount":460.00,
                 "original":{"currency":"USD","amount":100.00,"exchangeRate":4.60}}"""));

        assertThat(calls).hasSize(2);
        CashDepositedNotice deposit = (CashDepositedNotice) calls.get(0);
        assertThat(deposit.ownerId()).isEqualTo("bob");
        assertThat(deposit.amount()).isEqualByComparingTo("50.00");
        RemittanceReceivedNotice remittance = (RemittanceReceivedNotice) calls.get(1);
        assertThat(remittance.original().amount()).isEqualByComparingTo("100.00");
    }

    @Test
    @DisplayName("an event type this consumer doesn't know (e.g. added later) is skipped, not an error")
    void unknownType() {
        receive(envelope("ACCOUNT_OPENED", 1, "{\"accountNumber\":\"1000000003\"}"));

        assertThat(calls).isEmpty();
    }

    @Test
    @DisplayName("not JSON, a missing field, a bad UUID or an unknown version → UnprocessableMessageException (→ DLT)")
    void unprocessable() {
        assertThatThrownBy(() -> receive("this is not json")).isInstanceOf(UnprocessableMessageException.class);
        assertThatThrownBy(() -> receive(null)).isInstanceOf(UnprocessableMessageException.class);
        assertThatThrownBy(() -> receive(envelope("CASH_DEPOSITED", 1, "{\"reference\":\"CDM-1\"}")))
                .isInstanceOf(UnprocessableMessageException.class)
                .hasMessage("Missing field 'accountNumber'");
        assertThatThrownBy(() -> receive(envelope("CASH_DEPOSITED", 2, "{}")))
                .isInstanceOf(UnprocessableMessageException.class)
                .hasMessage("CASH_DEPOSITED version 2 is not supported");
        assertThatThrownBy(() -> receive(envelope("CASH_DEPOSITED", 1, """
                {"reference":"CDM-1","accountNumber":"1000000002","ownerId":"bob","amount":"fifty"}""")))
                .isInstanceOf(UnprocessableMessageException.class);
        assertThatThrownBy(() -> receive("{\"eventId\":\"nope\",\"eventType\":\"CASH_DEPOSITED\",\"version\":1}"))
                .isInstanceOf(UnprocessableMessageException.class);
        assertThat(calls).isEmpty();
    }
}
