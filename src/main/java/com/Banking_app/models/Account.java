package com.Banking_app.models;

import com.Banking_app.models.enums.AccountCurrency;
import com.Banking_app.models.enums.AccountStatus;
import com.Banking_app.models.enums.AccountType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.CascadeType;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Entity
@Getter
@Setter
@Table(name = "accounts")
public class Account {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long accountId;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private UserProfile user;
    @Column(name = "account_number",unique = true, nullable = false, length = 30)
    private String accountNumber;
    @Enumerated(EnumType.STRING)
    @Column(name = "account_type",nullable = false, length = 30)
    private AccountType accountType;
    @Enumerated(EnumType.STRING)
    @Column(name = "account_status",nullable = false, length = 30)
    private AccountStatus accountStatus;
    @Enumerated(EnumType.STRING)
    @Column(name = "account_currency",nullable = false, length = 3)
    private AccountCurrency accountCurrency = AccountCurrency.MYR;;
    @Column(name = "created_at",nullable = false)
    private Instant createdAt;
    @Column(name = "current_balance",nullable = false, precision = 19, scale = 2)
    private BigDecimal currentBalance;
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "account", cascade = CascadeType.ALL, orphanRemoval = true )
    private List<LedgerEntry> ledgerEntries;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "fromAccount", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Transaction> outgoingTransactions;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "toAccount", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Transaction> incomingTransactions;

    public Account(Long accountId, UserProfile user, String accountNumber, AccountType accountType, AccountStatus accountStatus, Instant createdAt, BigDecimal currentBalance){
        this.accountId = accountId;
        this.user = user;
        this.accountNumber = accountNumber;
        this.accountType = accountType;
        this.accountStatus = accountStatus;
        this.createdAt = createdAt;
        this.currentBalance = currentBalance;
    }
    public Account(){}
}
