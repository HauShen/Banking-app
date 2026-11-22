package com.Banking_app.alert.adapter.out.persistence;

import com.Banking_app.alert.adapter.out.persistence.entities.TransactionAlertJpaEntity;
import com.Banking_app.alert.adapter.out.persistence.jparepositories.TransactionAlertJpaRepository;
import com.Banking_app.alert.application.port.out.LoadAlertPort;
import com.Banking_app.alert.application.port.out.SaveAlertPort;
import com.Banking_app.alert.domain.enums.AlertRule;
import com.Banking_app.alert.domain.enums.AlertStatus;
import com.Banking_app.alert.domain.models.TransactionAlert;
import com.Banking_app.shared.PageResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;

@Component
public class TransactionAlertPersistenceAdapter implements SaveAlertPort, LoadAlertPort {

    private static final Sort NEWEST_FIRST = Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));

    private final TransactionAlertJpaRepository repository;

    public TransactionAlertPersistenceAdapter(TransactionAlertJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public TransactionAlert save(TransactionAlert alert) {
        return toDomain(repository.save(toEntity(alert)));
    }

    @Override
    public PageResult<TransactionAlert> findPage(AlertStatus status, int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size, NEWEST_FIRST);
        Page<TransactionAlertJpaEntity> rows = status == null
                ? repository.findAll(pageRequest)
                : repository.findByStatus(status, pageRequest);
        return new PageResult<>(rows.getContent().stream().map(TransactionAlertPersistenceAdapter::toDomain).toList(),
                rows.getNumber(), rows.getSize(), rows.getTotalElements());
    }

    @Override
    public long countByStatus(AlertStatus status) {
        return repository.countByStatus(status);
    }

    @Override
    public Optional<TransactionAlert> findById(long id) {
        return repository.findById(id).map(TransactionAlertPersistenceAdapter::toDomain);
    }

    @Override
    public boolean existsAfter(AlertRule rule, String accountNumber, Instant after) {
        return repository.existsByRuleAndAccountNumberAndCreatedAtAfter(rule, accountNumber, after);
    }

    private static TransactionAlertJpaEntity toEntity(TransactionAlert a) {
        TransactionAlertJpaEntity e = new TransactionAlertJpaEntity();
        e.setId(a.getId());
        e.setEventId(a.getEventId());
        e.setRule(a.getRule());
        e.setAccountNumber(a.getAccountNumber());
        e.setOwnerId(a.getOwnerId());
        e.setReference(a.getReference());
        e.setAmount(a.getAmount());
        e.setDetail(a.getDetail());
        e.setCreatedAt(a.getCreatedAt());
        e.setStatus(a.getStatus());
        e.setReviewedAt(a.getReviewedAt());
        e.setReviewedBy(a.getReviewedBy());
        return e;
    }

    private static TransactionAlert toDomain(TransactionAlertJpaEntity e) {
        return TransactionAlert.rehydrate(e.getId(), e.getEventId(), e.getRule(), e.getAccountNumber(),
                e.getOwnerId(), e.getReference(), e.getAmount(), e.getDetail(), e.getCreatedAt(), e.getStatus(),
                e.getReviewedAt(), e.getReviewedBy());
    }
}
