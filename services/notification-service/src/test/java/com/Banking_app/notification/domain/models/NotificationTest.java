package com.Banking_app.notification.domain.models;

import com.Banking_app.notification.domain.enums.NotificationType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NotificationTest {

    private static final Instant AT = Instant.parse("2026-11-21T08:00:00Z");
    private static final UUID EVENT = UUID.randomUUID();

    @Test
    @DisplayName("sent: MYR amount, the foreign amount in brackets, the recipient's account masked to its last 4 digits")
    void moneySent() {
        Notification n = Notification.moneySent(EVENT, "alice", "TXN-1", new BigDecimal("95.00"),
                new ForeignAmount("USD", new BigDecimal("20.00")), "1000000002", AT);

        assertThat(n.getType()).isEqualTo(NotificationType.MONEY_SENT);
        assertThat(n.getMessage()).isEqualTo("You sent RM 95.00 (USD 20.00) to account ****0002.");
        assertThat(n.getUserId()).isEqualTo("alice");
        assertThat(n.getReference()).isEqualTo("TXN-1");
        assertThat(n.getEventId()).isEqualTo(EVENT);
        assertThat(n.getCreatedAt()).isEqualTo(AT);
        assertThat(n.isRead()).isFalse();
        assertThat(n.getId()).isNull();
    }

    @Test
    @DisplayName("received, deposited, from abroad: one sentence each; a MYR amount always shows 2 decimals")
    void otherTexts() {
        assertThat(Notification.moneyReceived(EVENT, "bob", "TXN-1", new BigDecimal("30"), "1000000001", AT)
                .getMessage()).isEqualTo("You received RM 30.00 from account ****0001.");
        assertThat(Notification.cashDeposited(EVENT, "bob", "CDM-1", new BigDecimal("50.00"), "1000000002", AT)
                .getMessage()).isEqualTo("RM 50.00 was deposited into your account ****0002.");
        assertThat(Notification.remittanceReceived(EVENT, "bob", "RMT-1", new BigDecimal("460.00"),
                new ForeignAmount("USD", new BigDecimal("100.00")), "1000000002", AT).getMessage())
                .isEqualTo("You received RM 460.00 (USD 100.00) from abroad in account ****0002.");
    }

    @Test
    @DisplayName("markRead returns a read copy; marking it again keeps the first read time")
    void markRead() {
        Notification unread = Notification.cashDeposited(EVENT, "bob", "CDM-1", new BigDecimal("50.00"),
                "1000000002", AT);

        Notification read = unread.markRead(AT.plusSeconds(60));
        Notification again = read.markRead(AT.plusSeconds(120));

        assertThat(unread.isRead()).isFalse();
        assertThat(read.getReadAt()).isEqualTo(AT.plusSeconds(60));
        assertThat(again.getReadAt()).isEqualTo(AT.plusSeconds(60));
    }

    @Test
    @DisplayName("a zero or negative amount, or a missing user, is refused")
    void invalid() {
        assertThatThrownBy(() -> Notification.cashDeposited(EVENT, "bob", "CDM-1", BigDecimal.ZERO, "1000000002", AT))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Notification.cashDeposited(EVENT, null, "CDM-1", BigDecimal.ONE, "1000000002", AT))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("mask: last 4 digits only; very short numbers are shown as they are")
    void mask() {
        assertThat(Notification.mask("1000000002")).isEqualTo("****0002");
        assertThat(Notification.mask("1234")).isEqualTo("1234");
    }
}
