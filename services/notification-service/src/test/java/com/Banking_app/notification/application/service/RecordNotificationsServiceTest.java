package com.Banking_app.notification.application.service;

import com.Banking_app.notification.application.port.in.command.CashDepositedNotice;
import com.Banking_app.notification.application.port.in.command.RemittanceReceivedNotice;
import com.Banking_app.notification.application.port.in.command.TransferCompletedNotice;
import com.Banking_app.notification.application.port.out.ProcessedEventsPort;
import com.Banking_app.notification.domain.enums.NotificationType;
import com.Banking_app.notification.domain.models.ForeignAmount;
import com.Banking_app.notification.domain.models.Notification;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** Use-case tests with fakes: no Spring, no database, no Kafka. */
class RecordNotificationsServiceTest {

    private static final Instant MOVED_AT = Instant.parse("2026-11-21T08:00:00Z");
    private static final Instant NOW = Instant.parse("2026-11-21T08:00:02Z");

    private final FakeNotificationStore store = new FakeNotificationStore();
    private final Set<String> processed = new HashSet<>();
    private final ProcessedEventsPort processedEvents = (consumer, eventId, at) -> processed.add(consumer + "/" + eventId);
    private final RecordNotificationsService service = new RecordNotificationsService(processedEvents, store,
            Clock.fixed(NOW, ZoneOffset.UTC));

    private static TransferCompletedNotice transfer(UUID eventId, ForeignAmount original) {
        return new TransferCompletedNotice(eventId, MOVED_AT, "TXN-1", "1000000001", "alice", "1000000002", "bob",
                new BigDecimal("30.00"), original);
    }

    @Test
    @DisplayName("a transfer → 'sent' for the sender and 'received' for the recipient, at the time the money moved")
    void transfer() {
        UUID eventId = UUID.randomUUID();

        service.transferCompleted(transfer(eventId, null));

        assertThat(store.rows).hasSize(2);
        Notification sent = store.rows.get(0);
        assertThat(sent.getUserId()).isEqualTo("alice");
        assertThat(sent.getType()).isEqualTo(NotificationType.MONEY_SENT);
        assertThat(sent.getMessage()).isEqualTo("You sent RM 30.00 to account ****0002.");
        Notification received = store.rows.get(1);
        assertThat(received.getUserId()).isEqualTo("bob");
        assertThat(received.getType()).isEqualTo(NotificationType.MONEY_RECEIVED);
        assertThat(received.getMessage()).isEqualTo("You received RM 30.00 from account ****0001.");
        assertThat(store.rows).allSatisfy(n -> {
            assertThat(n.getReference()).isEqualTo("TXN-1");
            assertThat(n.getAmount()).isEqualByComparingTo("30.00");
            assertThat(n.getCreatedAt()).isEqualTo(MOVED_AT);
            assertThat(n.getEventId()).isEqualTo(eventId);
            assertThat(n.isRead()).isFalse();
        });
        assertThat(processed).containsExactly("notifications/" + eventId);
    }

    @Test
    @DisplayName("the same event delivered again (at-least-once) → nothing new")
    void redelivery() {
        TransferCompletedNotice notice = transfer(UUID.randomUUID(), null);

        service.transferCompleted(notice);
        service.transferCompleted(notice);

        assertThat(store.rows).hasSize(2);
    }

    @Test
    @DisplayName("a transfer paid in USD: the sender sees the USD amount too, the recipient only what arrived in MYR")
    void foreignTransfer() {
        service.transferCompleted(transfer(UUID.randomUUID(), new ForeignAmount("USD", new BigDecimal("6.30"))));

        assertThat(store.rows.get(0).getMessage()).isEqualTo("You sent RM 30.00 (USD 6.30) to account ****0002.");
        assertThat(store.rows.get(1).getMessage()).isEqualTo("You received RM 30.00 from account ****0001.");
    }

    @Test
    @DisplayName("a top-up → one CASH_DEPOSITED for the owner")
    void cashDeposit() {
        service.cashDeposited(new CashDepositedNotice(UUID.randomUUID(), MOVED_AT, "CDM-1", "1000000002", "bob",
                new BigDecimal("50.00")));

        assertThat(store.rows).singleElement().satisfies(n -> {
            assertThat(n.getUserId()).isEqualTo("bob");
            assertThat(n.getType()).isEqualTo(NotificationType.CASH_DEPOSITED);
            assertThat(n.getMessage()).isEqualTo("RM 50.00 was deposited into your account ****0002.");
        });
    }

    @Test
    @DisplayName("a remittance → one REMITTANCE_RECEIVED with the foreign amount; a redelivery adds nothing")
    void remittance() {
        RemittanceReceivedNotice notice = new RemittanceReceivedNotice(UUID.randomUUID(), MOVED_AT, "RMT-1",
                "1000000002", "bob", new BigDecimal("460.00"), new ForeignAmount("USD", new BigDecimal("100.00")));

        service.remittanceReceived(notice);
        service.remittanceReceived(notice);

        assertThat(store.rows).singleElement().satisfies(n -> {
            assertThat(n.getType()).isEqualTo(NotificationType.REMITTANCE_RECEIVED);
            assertThat(n.getMessage()).isEqualTo("You received RM 460.00 (USD 100.00) from abroad in account ****0002.");
        });
    }
}
