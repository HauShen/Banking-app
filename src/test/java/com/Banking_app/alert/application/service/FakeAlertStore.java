package com.Banking_app.alert.application.service;

import com.Banking_app.alert.application.port.out.LoadAlertPort;
import com.Banking_app.alert.application.port.out.SaveAlertPort;
import com.Banking_app.alert.domain.enums.AlertRule;
import com.Banking_app.alert.domain.enums.AlertStatus;
import com.Banking_app.alert.domain.models.TransactionAlert;
import com.Banking_app.shared.PageResult;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** The transaction_alerts table in memory. */
class FakeAlertStore implements SaveAlertPort, LoadAlertPort {

    final List<TransactionAlert> rows = new ArrayList<>();
    private long nextId = 1;

    @Override
    public TransactionAlert save(TransactionAlert alert) {
        if (alert.getId() == null) {
            TransactionAlert stored = withId(alert, nextId++);
            rows.add(stored);
            return stored;
        }
        rows.replaceAll(a -> a.getId().equals(alert.getId()) ? alert : a);
        return alert;
    }

    @Override
    public PageResult<TransactionAlert> findPage(AlertStatus status, int page, int size) {
        List<TransactionAlert> matching = rows.stream()
                .filter(a -> status == null || a.getStatus() == status)
                .sorted(Comparator.comparing(TransactionAlert::getCreatedAt).thenComparing(TransactionAlert::getId)
                        .reversed())
                .toList();
        return new PageResult<>(matching.stream().skip((long) page * size).limit(size).toList(), page, size,
                matching.size());
    }

    @Override
    public long countByStatus(AlertStatus status) {
        return rows.stream().filter(a -> a.getStatus() == status).count();
    }

    @Override
    public Optional<TransactionAlert> findById(long id) {
        return rows.stream().filter(a -> a.getId() == id).findFirst();
    }

    @Override
    public boolean existsAfter(AlertRule rule, String accountNumber, Instant after) {
        return rows.stream().anyMatch(a -> a.getRule() == rule && a.getAccountNumber().equals(accountNumber)
                && a.getCreatedAt().isAfter(after));
    }

    List<TransactionAlert> ofRule(AlertRule rule) {
        return rows.stream().filter(a -> a.getRule() == rule).toList();
    }

    private static TransactionAlert withId(TransactionAlert a, long id) {
        return TransactionAlert.rehydrate(id, a.getEventId(), a.getRule(), a.getAccountNumber(), a.getOwnerId(),
                a.getReference(), a.getAmount(), a.getDetail(), a.getCreatedAt(), a.getStatus(), a.getReviewedAt(),
                a.getReviewedBy());
    }
}
