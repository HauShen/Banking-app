package com.Banking_app.alert.application.service;

import com.Banking_app.alert.application.exception.AlertNotFoundException;
import com.Banking_app.alert.application.port.in.query.ListAlertsQuery;
import com.Banking_app.alert.application.port.in.result.AlertView;
import com.Banking_app.alert.domain.enums.AlertRule;
import com.Banking_app.alert.domain.enums.AlertStatus;
import com.Banking_app.alert.domain.models.TransactionAlert;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AlertQueriesTest {

    private static final Instant T0 = Instant.parse("2026-11-21T08:00:00Z");
    private static final Instant NOW = Instant.parse("2026-11-21T09:00:00Z");

    private final FakeAlertStore store = new FakeAlertStore();
    private final ListAlertsService list = new ListAlertsService(store);
    private final ReviewAlertService review = new ReviewAlertService(store, store, Clock.fixed(NOW, ZoneOffset.UTC));

    private TransactionAlert alert(String reference, Instant at) {
        return store.save(TransactionAlert.rehydrate(null, UUID.randomUUID(), AlertRule.LARGE_TRANSFER, "1000000001",
                "alice", reference, new BigDecimal("12000.00"), "large", at, AlertStatus.OPEN, null, null));
    }

    @Test
    @DisplayName("list: newest first, filtered by status or all; the open count")
    void listAndCount() {
        alert("TXN-OLD", T0);
        TransactionAlert middle = alert("TXN-MID", T0.plusSeconds(60));
        alert("TXN-NEW", T0.plusSeconds(120));
        review.review(middle.getId(), "admin-1");

        assertThat(list.list(new ListAlertsQuery(null, 0, 20)).content()).extracting(AlertView::reference)
                .containsExactly("TXN-NEW", "TXN-MID", "TXN-OLD");
        assertThat(list.list(new ListAlertsQuery(AlertStatus.OPEN, 0, 20)).content()).extracting(AlertView::reference)
                .containsExactly("TXN-NEW", "TXN-OLD");
        assertThat(list.list(new ListAlertsQuery(AlertStatus.REVIEWED, 0, 20)).content())
                .extracting(AlertView::reference).containsExactly("TXN-MID");
        assertThat(list.countOpen()).isEqualTo(2);
    }

    @Test
    @DisplayName("review: reviewed by this admin now; again by another admin → still the first review")
    void reviewOnce() {
        TransactionAlert a = alert("TXN-1", T0);

        AlertView first = review.review(a.getId(), "admin-1");
        AlertView second = review.review(a.getId(), "admin-2");

        assertThat(first.status()).isEqualTo(AlertStatus.REVIEWED);
        assertThat(first.reviewedBy()).isEqualTo("admin-1");
        assertThat(first.reviewedAt()).isEqualTo(NOW);
        assertThat(second.reviewedBy()).isEqualTo("admin-1");
    }

    @Test
    @DisplayName("review of an unknown alert → 404; bad paging → 400")
    void errors() {
        assertThatThrownBy(() -> review.review(999L, "admin-1")).isInstanceOf(AlertNotFoundException.class)
                .hasMessage("Alert not found");
        assertThatThrownBy(() -> new ListAlertsQuery(null, -1, 20)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ListAlertsQuery(null, 0, 101)).isInstanceOf(IllegalArgumentException.class);
    }
}
