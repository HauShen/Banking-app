package com.Banking_app.alert.domain.models;

import com.Banking_app.alert.domain.enums.AlertRule;
import com.Banking_app.alert.domain.enums.AlertStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AlertRulesTest {

    private static final Instant AT = Instant.parse("2026-11-21T08:00:00Z");
    private final AlertRules rules = new AlertRules(new BigDecimal("10000.00"), 5, Duration.ofMinutes(10));

    private static ObservedTransfer transfer(String amount) {
        return new ObservedTransfer(UUID.randomUUID(), AT, "TXN-1", "1000000001", "alice", "1000000002",
                new BigDecimal(amount));
    }

    @Test
    @DisplayName("large transfer: exactly the limit or more → an open alert about the sending account; 1 sen less → none")
    void largeTransfer() {
        assertThat(rules.checkLargeTransfer(transfer("9999.99"))).isEmpty();

        TransactionAlert alert = rules.checkLargeTransfer(transfer("10000")).orElseThrow();

        assertThat(alert.getRule()).isEqualTo(AlertRule.LARGE_TRANSFER);
        assertThat(alert.getStatus()).isEqualTo(AlertStatus.OPEN);
        assertThat(alert.getAccountNumber()).isEqualTo("1000000001");
        assertThat(alert.getOwnerId()).isEqualTo("alice");
        assertThat(alert.getReference()).isEqualTo("TXN-1");
        assertThat(alert.getCreatedAt()).isEqualTo(AT);
        assertThat(alert.getDetail()).isEqualTo("Transfer of RM 10000.00 is at or above the RM 10000.00 limit");
    }

    @Test
    @DisplayName("rapid transfers: the 6th in the window → alert; the 5th → none; already alerted in this burst → none")
    void rapidTransfers() {
        assertThat(rules.checkRapidTransfers(transfer("10.00"), 5, false)).isEmpty();
        assertThat(rules.checkRapidTransfers(transfer("10.00"), 7, true)).isEmpty();

        TransactionAlert alert = rules.checkRapidTransfers(transfer("10.00"), 6, false).orElseThrow();

        assertThat(alert.getRule()).isEqualTo(AlertRule.RAPID_TRANSFERS);
        assertThat(alert.getDetail()).isEqualTo("6 transfers from this account within 10 minutes (limit 5)");
    }

    @Test
    @DisplayName("the window ends at the transfer and reaches back the configured time")
    void windowStart() {
        assertThat(rules.windowStart(AT)).isEqualTo(AT.minus(Duration.ofMinutes(10)));
    }

    @Test
    @DisplayName("window text: minutes when whole, else seconds")
    void describe() {
        assertThat(AlertRules.describe(Duration.ofMinutes(1))).isEqualTo("1 minute");
        assertThat(AlertRules.describe(Duration.ofSeconds(90))).isEqualTo("90 seconds");
    }

    @Test
    @DisplayName("limits must be positive")
    void invalidLimits() {
        assertThatThrownBy(() -> new AlertRules(BigDecimal.ZERO, 5, Duration.ofMinutes(10)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new AlertRules(BigDecimal.TEN, 0, Duration.ofMinutes(10)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new AlertRules(BigDecimal.TEN, 5, Duration.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("review: reviewed by the admin at that time; reviewing again keeps the first review")
    void review() {
        TransactionAlert open = rules.checkLargeTransfer(transfer("20000.00")).orElseThrow();

        TransactionAlert reviewed = open.review("admin-1", AT.plusSeconds(60));
        TransactionAlert again = reviewed.review("admin-2", AT.plusSeconds(120));

        assertThat(reviewed.getStatus()).isEqualTo(AlertStatus.REVIEWED);
        assertThat(reviewed.getReviewedBy()).isEqualTo("admin-1");
        assertThat(again.getReviewedBy()).isEqualTo("admin-1");
        assertThat(again.getReviewedAt()).isEqualTo(AT.plusSeconds(60));
        assertThat(open.isReviewed()).isFalse();
    }
}
