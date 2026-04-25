package com.Banking_app.models;

import com.Banking_app.models.enums.LedgerType;
import com.fasterxml.jackson.annotation.JsonIgnore;
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
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Getter
@Setter
public class Ledger {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    @JsonIgnore
    private Account account;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id", nullable = false)
    @JsonIgnore
    private Transaction transaction;

    @Enumerated(EnumType.STRING)
    @Column(name = "ledger_type")
    private LedgerType ledgerType;
    @Column(name = "amount")
    private BigDecimal amount;
    @Column(name = "balance_after")
    private BigDecimal balanceAfter;
    @Column(name = "created_at")
    private Instant createdAt;
    public Ledger(Long id, Account account, Transaction transaction, LedgerType ledgerType, BigDecimal amount, BigDecimal balanceAfter, Instant createdAt){
        this.id = id;
        this.account = account;
        this.transaction = transaction;
        this.ledgerType = ledgerType;
        this.amount =amount;
        this.balanceAfter = balanceAfter;
        this.createdAt = createdAt;
    }

    public Ledger(){}
}
