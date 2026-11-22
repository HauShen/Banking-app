package com.Banking_app.notification.application.service;

import com.Banking_app.notification.application.port.out.LoadNotificationPort;
import com.Banking_app.notification.application.port.out.SaveNotificationPort;
import com.Banking_app.notification.domain.models.Notification;
import com.Banking_app.notification.shared.PageResult;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** The notifications table in memory. */
class FakeNotificationStore implements SaveNotificationPort, LoadNotificationPort {

    final List<Notification> rows = new ArrayList<>();
    private long nextId = 1;

    /** Stores a notification as if it had been saved earlier; returns it with its id. */
    Notification add(Notification notification) {
        Notification stored = withId(notification, nextId++);
        rows.add(stored);
        return stored;
    }

    Notification byId(long id) {
        return rows.stream().filter(n -> n.getId() == id).findFirst().orElseThrow();
    }

    @Override
    public void saveAll(List<Notification> notifications) {
        notifications.forEach(this::add);
    }

    @Override
    public void save(Notification notification) {
        rows.replaceAll(n -> n.getId().equals(notification.getId()) ? notification : n);
    }

    @Override
    public int markAllRead(String userId, Instant at) {
        int[] marked = {0};
        rows.replaceAll(n -> {
            if (n.belongsTo(userId) && !n.isRead()) {
                marked[0]++;
                return n.markRead(at);
            }
            return n;
        });
        return marked[0];
    }

    @Override
    public PageResult<Notification> findPage(String userId, boolean unreadOnly, int page, int size) {
        List<Notification> matching = rows.stream()
                .filter(n -> n.belongsTo(userId))
                .filter(n -> !unreadOnly || !n.isRead())
                .sorted(Comparator.comparing(Notification::getCreatedAt).thenComparing(Notification::getId).reversed())
                .toList();
        List<Notification> content = matching.stream().skip((long) page * size).limit(size).toList();
        return new PageResult<>(content, page, size, matching.size());
    }

    @Override
    public long countUnread(String userId) {
        return rows.stream().filter(n -> n.belongsTo(userId) && !n.isRead()).count();
    }

    @Override
    public Optional<Notification> findById(long id) {
        return rows.stream().filter(n -> n.getId() == id).findFirst();
    }

    private static Notification withId(Notification n, long id) {
        return Notification.rehydrate(id, n.getEventId(), n.getUserId(), n.getType(), n.getMessage(),
                n.getReference(), n.getAmount(), n.getCreatedAt(), n.getReadAt());
    }
}
