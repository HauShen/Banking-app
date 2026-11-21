package com.Banking_app.notification.application.service;

import com.Banking_app.notification.application.exception.NotificationNotFoundException;
import com.Banking_app.notification.application.port.in.query.ListNotificationsQuery;
import com.Banking_app.notification.application.port.in.result.NotificationView;
import com.Banking_app.notification.domain.models.Notification;
import com.Banking_app.shared.PageResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Listing, counting and marking as read, with the fake store. */
class NotificationQueriesTest {

    private static final Instant T0 = Instant.parse("2026-11-21T08:00:00Z");
    private static final Instant NOW = Instant.parse("2026-11-21T09:00:00Z");

    private final FakeNotificationStore store = new FakeNotificationStore();
    private final ListNotificationsService list = new ListNotificationsService(store);
    private final MarkNotificationsReadService mark = new MarkNotificationsReadService(store, store,
            Clock.fixed(NOW, ZoneOffset.UTC));

    private Notification deposit(String userId, String reference, Instant at) {
        return store.add(Notification.cashDeposited(UUID.randomUUID(), userId, reference, new BigDecimal("10.00"),
                "1000000001", at));
    }

    @Test
    @DisplayName("list: only the user's own, newest first; unreadOnly leaves out read ones; the count matches")
    void listAndCount() {
        deposit("bob", "CDM-OLD", T0);
        Notification middle = deposit("bob", "CDM-MID", T0.plusSeconds(60));
        deposit("bob", "CDM-NEW", T0.plusSeconds(120));
        deposit("alice", "CDM-ALICE", T0.plusSeconds(180));
        mark.markRead("bob", middle.getId());

        PageResult<NotificationView> all = list.list(new ListNotificationsQuery("bob", false, 0, 20));
        PageResult<NotificationView> unread = list.list(new ListNotificationsQuery("bob", true, 0, 20));

        assertThat(all.content()).extracting(NotificationView::reference)
                .containsExactly("CDM-NEW", "CDM-MID", "CDM-OLD");
        assertThat(unread.content()).extracting(NotificationView::reference).containsExactly("CDM-NEW", "CDM-OLD");
        assertThat(list.countUnread("bob")).isEqualTo(2);
        assertThat(list.countUnread("alice")).isEqualTo(1);
    }

    @Test
    @DisplayName("markRead: the owner's notification becomes read now; again → still the first time")
    void markRead() {
        Notification n = deposit("bob", "CDM-1", T0);

        mark.markRead("bob", n.getId());

        assertThat(store.byId(n.getId()).getReadAt()).isEqualTo(NOW);
        mark.markRead("bob", n.getId());
        assertThat(store.byId(n.getId()).getReadAt()).isEqualTo(NOW);
    }

    @Test
    @DisplayName("markRead of someone else's or an unknown notification → the same 404; nothing changes")
    void markReadNotFound() {
        Notification bobs = deposit("bob", "CDM-1", T0);

        assertThatThrownBy(() -> mark.markRead("alice", bobs.getId()))
                .isInstanceOf(NotificationNotFoundException.class)
                .hasMessage("Notification not found");
        assertThatThrownBy(() -> mark.markRead("bob", 999L)).isInstanceOf(NotificationNotFoundException.class);
        assertThat(store.byId(bobs.getId()).isRead()).isFalse();
    }

    @Test
    @DisplayName("markAllRead: all of the user's unread, nobody else's; returns how many")
    void markAllRead() {
        deposit("bob", "CDM-1", T0);
        deposit("bob", "CDM-2", T0);
        deposit("alice", "CDM-3", T0);

        assertThat(mark.markAllRead("bob")).isEqualTo(2);
        assertThat(mark.markAllRead("bob")).isZero();
        assertThat(list.countUnread("bob")).isZero();
        assertThat(list.countUnread("alice")).isEqualTo(1);
    }

    @Test
    @DisplayName("paging: page < 0, size < 1 or size > 100 → 400 (IllegalArgumentException)")
    void badPaging() {
        assertThatThrownBy(() -> new ListNotificationsQuery("bob", false, -1, 20))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("page must not be negative");
        assertThatThrownBy(() -> new ListNotificationsQuery("bob", false, 0, 0))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("size must be between 1 and 100");
        assertThatThrownBy(() -> new ListNotificationsQuery("bob", false, 0, 101))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
