package com.Banking_app.account.adapter.out.persistence.entities;

import com.Banking_app.account.domain.enums.AccountCurrency;
import com.Banking_app.account.domain.enums.AccountStatus;
import com.Banking_app.account.domain.enums.AccountType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * JPA mapping of the "accounts" table. Lives in the adapter: the domain never sees it.
 * - user_id is a plain column (no link to UserProfileJpaEntity); the DB foreign key stays.
 * - No ledger/transaction collections and no cascade: deleting an account with history is
 *   blocked by the DB (ON DELETE RESTRICT) instead of wiping the history.
 */
@Entity
@Getter
@Setter
@Table(name = "accounts")
public class AccountJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long accountId;

    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;

    @Column(name = "account_number", unique = true, nullable = false, length = 30)
    private String accountNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false, length = 30)
    private AccountType accountType;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_status", nullable = false, length = 30)
    private AccountStatus accountStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_currency", nullable = false, length = 3)
    private AccountCurrency accountCurrency = AccountCurrency.MYR;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "current_balance", nullable = false, precision = 19, scale = 2)
    private BigDecimal currentBalance;

    public AccountJpaEntity() {
    }
}
