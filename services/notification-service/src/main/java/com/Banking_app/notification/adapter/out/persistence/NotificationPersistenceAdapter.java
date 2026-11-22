package com.Banking_app.notification.adapter.out.persistence;

import com.Banking_app.notification.adapter.out.persistence.entities.NotificationJpaEntity;
import com.Banking_app.notification.adapter.out.persistence.jparepositories.NotificationJpaRepository;
import com.Banking_app.notification.application.port.out.LoadNotificationPort;
import com.Banking_app.notification.application.port.out.SaveNotificationPort;
import com.Banking_app.notification.domain.models.Notification;
import com.Banking_app.notification.shared.PageResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Component
public class NotificationPersistenceAdapter implements SaveNotificationPort, LoadNotificationPort {

    private static final Sort NEWEST_FIRST = Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));

    private final NotificationJpaRepository repository;

    public NotificationPersistenceAdapter(NotificationJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public void saveAll(List<Notification> notifications) {
        repository.saveAll(notifications.stream().map(NotificationPersistenceAdapter::toEntity).toList());
    }

    @Override
    public void save(Notification notification) {
        repository.save(toEntity(notification));
    }

    @Override
    public int markAllRead(String userId, Instant at) {
        return repository.markAllRead(userId, at);
    }

    @Override
    public PageResult<Notification> findPage(String userId, boolean unreadOnly, int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size, NEWEST_FIRST);
        Page<NotificationJpaEntity> rows = unreadOnly
                ? repository.findByUserIdAndReadAtIsNull(userId, pageRequest)
                : repository.findByUserId(userId, pageRequest);
        return new PageResult<>(rows.getContent().stream().map(NotificationPersistenceAdapter::toDomain).toList(),
                rows.getNumber(), rows.getSize(), rows.getTotalElements());
    }

    @Override
    public long countUnread(String userId) {
        return repository.countByUserIdAndReadAtIsNull(userId);
    }

    @Override
    public Optional<Notification> findById(long id) {
        return repository.findById(id).map(NotificationPersistenceAdapter::toDomain);
    }

    private static NotificationJpaEntity toEntity(Notification n) {
        NotificationJpaEntity entity = new NotificationJpaEntity();
        entity.setId(n.getId());
        entity.setEventId(n.getEventId());
        entity.setUserId(n.getUserId());
        entity.setType(n.getType());
        entity.setMessage(n.getMessage());
        entity.setReference(n.getReference());
        entity.setAmount(n.getAmount());
        entity.setCreatedAt(n.getCreatedAt());
        entity.setReadAt(n.getReadAt());
        return entity;
    }

    private static Notification toDomain(NotificationJpaEntity e) {
        return Notification.rehydrate(e.getId(), e.getEventId(), e.getUserId(), e.getType(), e.getMessage(),
                e.getReference(), e.getAmount(), e.getCreatedAt(), e.getReadAt());
    }
}
