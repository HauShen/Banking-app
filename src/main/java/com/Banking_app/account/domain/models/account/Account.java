package com.Banking_app.account.domain.models.account;

import com.Banking_app.account.domain.enums.AccountCurrency;
import com.Banking_app.account.domain.enums.AccountStatus;
import com.Banking_app.account.domain.enums.AccountType;
import com.Banking_app.account.domain.exception.AccountNotActiveException;
import com.Banking_app.account.domain.exception.InsufficientBalanceException;
import com.Banking_app.account.domain.exception.InvalidAmountException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

/**
 * Bank account aggregate. Pure Java: no Spring, no JPA, no setters.
 * State only changes through methods that enforce the business rules.
 */
public class Account {

    public static final BigDecimal OPENING_BALANCE = new BigDecimal("20.00");

    /** A withdrawal may not take the balance below this. Same value as the opening balance, different rule. */
    public static final BigDecimal MINIMUM_BALANCE = new BigDecimal("20.00");

    /** Money has at most 2 decimals (sen); the balance column is NUMERIC(19,2). */
    public static final int MAX_DECIMALS = 2;

    private final Long id;                  // null until the account is saved
    private final String ownerId;           // UserProfile id (plain value, no link to the user module)
    private final String accountNumber;
    private final AccountType type;
    private AccountStatus status;
    private final AccountCurrency currency;
    private BigDecimal balance;
    private final Instant createdAt;

    private Account(Long id, String ownerId, String accountNumber, AccountType type,
                    AccountStatus status, AccountCurrency currency, BigDecimal balance, Instant createdAt) {
        this.id = id;
        this.ownerId = Objects.requireNonNull(ownerId, "ownerId is required");
        this.accountNumber = Objects.requireNonNull(accountNumber, "accountNumber is required");
        this.type = Objects.requireNonNull(type, "accountType is required");
        this.status = Objects.requireNonNull(status, "accountStatus is required");
        this.currency = Objects.requireNonNull(currency, "accountCurrency is required");
        this.balance = Objects.requireNonNull(balance, "balance is required");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt is required");
    }

    /** Opens a new account: ACTIVE, MYR, with the opening balance. */
    public static Account open(String ownerId, String accountNumber, AccountType type, Instant now) {
        return new Account(null, ownerId, accountNumber, type,
                AccountStatus.ACTIVE, AccountCurrency.MYR, OPENING_BALANCE, now);
    }

    /** Rebuilds an existing account from storage. Used only by the persistence mapper. */
    public static Account rehydrate(Long id, String ownerId, String accountNumber, AccountType type,
                                    AccountStatus status, AccountCurrency currency,
                                    BigDecimal balance, Instant createdAt) {
        Objects.requireNonNull(id, "id is required for an existing account");
        return new Account(id, ownerId, accountNumber, type, status, currency, balance, createdAt);
    }

    /** Adds money to the account. The amount must be positive with at most 2 decimals, and the account ACTIVE. */
    public void deposit(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidAmountException("Top-up amount must be greater than zero");
        }
        if (hasTooManyDecimals(amount)) {
            throw new InvalidAmountException("Top-up amount can have at most 2 decimal places");
        }
        if (!isActive()) {
            throw new AccountNotActiveException("Cannot top up a non-active account");
        }
        this.balance = this.balance.add(amount);
    }

    /**
     * Takes money out of the account (the sending side of a transfer). The amount must be positive with at most
     * 2 decimals, the account ACTIVE, and at least {@link #MINIMUM_BALANCE} must stay in the account.
     * Same minimum-balance rule and message as the old TransactionServiceImpl.transfer.
     */
    public void withdraw(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidAmountException("Withdrawal amount must be greater than zero");
        }
        if (hasTooManyDecimals(amount)) {
            throw new InvalidAmountException("Withdrawal amount can have at most 2 decimal places");
        }
        if (!isActive()) {
            throw new AccountNotActiveException("Cannot withdraw from a non-active account");
        }
        BigDecimal remaining = this.balance.subtract(amount);
        if (remaining.compareTo(MINIMUM_BALANCE) < 0) {
            throw new InsufficientBalanceException(
                    "Insufficient balance. Minimum balance of $20.00 must be maintained.");
        }
        this.balance = remaining;
    }

    /**
     * Decimals as written: 10.5 and 10.50 are fine, 10.005 and 10.500 are not — the same rule as @Digits on the
     * transfer request, so every entry point agrees.
     * Before this rule, PostgreSQL silently rounded such amounts on save while Java had calculated with all of
     * them, so a balance and its ledger entry could disagree by 0.01.
     */
    private static boolean hasTooManyDecimals(BigDecimal amount) {
        return amount.scale() > MAX_DECIMALS;
    }

    public void changeStatus(AccountStatus newStatus) {
        this.status = Objects.requireNonNull(newStatus, "accountStatus is required");
    }

    public boolean isActive() {
        return status == AccountStatus.ACTIVE;
    }

    public boolean isOwnedBy(String userId) {
        return ownerId.equals(userId);
    }

    public Long getId() { return id; }
    public String getOwnerId() { return ownerId; }
    public String getAccountNumber() { return accountNumber; }
    public AccountType getType() { return type; }
    public AccountStatus getStatus() { return status; }
    public AccountCurrency getCurrency() { return currency; }
    public BigDecimal getBalance() { return balance; }
    public Instant getCreatedAt() { return createdAt; }
}
