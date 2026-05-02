package com.Banking_app.models;

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
import lombok.Data;
import jakarta.persistence.CascadeType;
import lombok.Getter;
import lombok.Setter;

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
    @Column(name = "account_number",unique = true, nullable = false)
    private String accountNumber;
    @Enumerated(EnumType.STRING)
    @Column(name = "account_type")
    private AccountType accountType;
    @Enumerated(EnumType.STRING)
    @Column(name = "account_status")
    private AccountStatus accountStatus;
    @Column(name = "created_at")
    private Instant createdAt;
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "account", cascade = CascadeType.ALL, orphanRemoval = true )
    private List<Ledger> ledgers;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "fromAccount", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Transaction> outgoingTransactions;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "toAccount", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Transaction> incomingTransactions;

    public Account(Long accountId, UserProfile user, String accountNumber, AccountType accountType, AccountStatus accountStatus, Instant createdAt){
        this.accountId = accountId;
        this.user = user;
        this.accountNumber = accountNumber;
        this.accountType = accountType;
        this.accountStatus = accountStatus;
        this.createdAt = createdAt;
    }
    public Account(){}
}
