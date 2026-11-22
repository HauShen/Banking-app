package com.Banking_app.alert.application.port.in.result;

import com.Banking_app.alert.domain.enums.AlertRule;
import com.Banking_app.alert.domain.enums.AlertStatus;
import com.Banking_app.alert.domain.models.TransactionAlert;

import java.math.BigDecimal;
import java.time.Instant;

public record AlertView(long id,
                        AlertRule rule,
                        AlertStatus status,
                        String accountNumber,
                        String ownerId,
                        String reference,
                        BigDecimal amount,
                        String detail,
                        Instant createdAt,
                        Instant reviewedAt,
                        String reviewedBy) {

    public static AlertView of(TransactionAlert alert) {
        return new AlertView(alert.getId(), alert.getRule(), alert.getStatus(), alert.getAccountNumber(),
                alert.getOwnerId(), alert.getReference(), alert.getAmount(), alert.getDetail(), alert.getCreatedAt(),
                alert.getReviewedAt(), alert.getReviewedBy());
    }
}
