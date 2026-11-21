package com.Banking_app.alert.adapter.out.persistence.entities;

import com.Banking_app.alert.domain.enums.AlertRule;
import com.Banking_app.alert.domain.enums.AlertStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Row of the {@code transaction_alerts} table (V14). */
@Entity
@Table(name = "transaction_alerts")
@Getter
@Setter
@NoArgsConstructor
public class TransactionAlertJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Enumerated(EnumType.STRING)
    @Column(name = "rule", nullable = false, length = 32)
    private AlertRule rule;

    @Column(name = "account_number", nullable = false, length = 30)
    private String accountNumber;

    @Column(name = "owner_id", nullable = false, length = 36)
    private String ownerId;

    @Column(name = "reference", nullable = false, length = 100)
    private String reference;

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "detail", nullable = false, length = 255)
    private String detail;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private AlertStatus status;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(name = "reviewed_by", length = 36)
    private String reviewedBy;
}
