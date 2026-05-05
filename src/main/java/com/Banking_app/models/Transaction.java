package com.Banking_app.models;

import com.Banking_app.models.enums.TransactionStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Entity
@Getter
@Setter
@Table(name = "transactions",uniqueConstraints = {
        @UniqueConstraint(name = "uk_transactions_reference", columnNames = "reference_number"),
        @UniqueConstraint(name = "uk_transactions_idempotency_key", columnNames = "idempotency_key")
        })
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "reference_number")
    private String referenceNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_account_id", nullable = false)
    @JsonIgnore
    Account fromAccount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_account_id", nullable = false)
    @JsonIgnore
    Account toAccount;
    @Column(name = "amount")
    private BigDecimal amount;
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private TransactionStatus status;

    @Column(name = "description")
    private String description;
    @Column(name = "created_at")
    private Instant createdAt;
    @Column(name = "success_at")
    private Instant successAt;
    @Column(name = "idempotency_key", nullable = false, length = 100, unique = true)
    private String idempotencyKey;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "transaction", cascade = CascadeType.ALL, orphanRemoval = true )
    private List<LedgerEntry> ledgerEntries;
    public Transaction(Long id,String referenceNumber,Account fromAccount, Account toAccount, BigDecimal amount, TransactionStatus status, String description, Instant createdAt, Instant successAt){
        this.id = id;
        this.referenceNumber = referenceNumber;
        this.fromAccount = fromAccount;
        this.toAccount = toAccount;
        this.amount = amount;
        this.status = status;
        this.description = description;
        this.createdAt = createdAt;
        this.successAt = successAt;
    }

    public Transaction(){}
}
