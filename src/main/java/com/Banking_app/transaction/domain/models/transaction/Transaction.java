package com.Banking_app.transaction.domain.models.transaction;

import com.Banking_app.transaction.domain.enums.TransactionStatus;
import com.Banking_app.transaction.domain.exception.InvalidTransactionAmountException;
import com.Banking_app.transaction.domain.exception.SameAccountTransferException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * A movement of money: a transfer between two accounts, or a cash deposit (top-up) into one account.
 * Aggregate root of the transaction module: its {@link LedgerEntry} lines only exist inside it.
 *
 * Pure Java (no JPA, Spring or Lombok) and immutable: every change returns a new Transaction, so a half-done
 * change can never be seen by anyone else.
 *
 * Life cycle:
 * <pre>
 *   startTransfer / startCashDeposit  → PENDING, no ledger entries
 *   completeTransfer                  → SUCCESS, DEBIT on the sender + CREDIT on the recipient
 *   completeCashDeposit               → SUCCESS, CREDIT on the recipient only (the money comes from the machine)
 * </pre>
 * FAILED exists in the enum and the database, but nothing stores it today: the old code's FAILED update was
 * rolled back together with the failed transfer (transaction plan, decision 7).
 *
 * Balances are not this class's job. The account module changes them and passes in the balances after,
 * which the ledger entries record.
 */
public final class Transaction {

    private final Long id;                 // null until saved
    private final String referenceNumber;
    private final Long fromAccountId;      // null for a cash deposit
    private final Long toAccountId;
    private final BigDecimal amount;
    private final String description;
    private final TransactionStatus status;
    private final Instant createdAt;
    private final Instant successAt;       // null until completed
    private final String idempotencyKey;
    private final List<LedgerEntry> ledgerEntries;

    private Transaction(Long id, String referenceNumber, Long fromAccountId, Long toAccountId, BigDecimal amount,
                        String description, TransactionStatus status, Instant createdAt, Instant successAt,
                        String idempotencyKey, List<LedgerEntry> ledgerEntries) {
        this.id = id;
        this.referenceNumber = referenceNumber;
        this.fromAccountId = fromAccountId;
        this.toAccountId = toAccountId;
        this.amount = amount;
        this.description = description;
        this.status = status;
        this.createdAt = createdAt;
        this.successAt = successAt;
        this.idempotencyKey = idempotencyKey;
        this.ledgerEntries = List.copyOf(ledgerEntries);   // unmodifiable copy
    }

    // ---------------------------------------------------------------- creating

    /** New PENDING transfer. The caller has already checked ownership, idempotency and the accounts' status. */
    public static Transaction startTransfer(String referenceNumber, Long fromAccountId, Long toAccountId,
                                            BigDecimal amount, String description, String idempotencyKey,
                                            Instant now) {
        Objects.requireNonNull(fromAccountId, "fromAccountId");
        Objects.requireNonNull(toAccountId, "toAccountId");
        if (fromAccountId.equals(toAccountId)) {
            throw new SameAccountTransferException();
        }
        return start(referenceNumber, fromAccountId, toAccountId, amount, description, idempotencyKey, now);
    }

    /** New PENDING cash deposit (top-up): money from the cash deposit machine, so there is no from-account. */
    public static Transaction startCashDeposit(String referenceNumber, Long toAccountId, BigDecimal amount,
                                               String description, String idempotencyKey, Instant now) {
        Objects.requireNonNull(toAccountId, "toAccountId");
        return start(referenceNumber, null, toAccountId, amount, description, idempotencyKey, now);
    }

    private static Transaction start(String referenceNumber, Long fromAccountId, Long toAccountId,
                                     BigDecimal amount, String description, String idempotencyKey, Instant now) {
        requireText(referenceNumber, "referenceNumber");
        requireText(idempotencyKey, "idempotencyKey");
        Objects.requireNonNull(now, "now");
        if (amount == null || amount.signum() <= 0) {
            throw new InvalidTransactionAmountException();
        }
        return new Transaction(null, referenceNumber, fromAccountId, toAccountId, amount, description,
                TransactionStatus.PENDING, now, null, idempotencyKey, List.of());
    }

    /**
     * Rebuilds a stored transaction (used by the persistence mapper). No business checks: the data was valid
     * when it was saved, and the database constraints guard it.
     */
    public static Transaction rehydrate(Long id, String referenceNumber, Long fromAccountId, Long toAccountId,
                                        BigDecimal amount, String description, TransactionStatus status,
                                        Instant createdAt, Instant successAt, String idempotencyKey,
                                        List<LedgerEntry> ledgerEntries) {
        return new Transaction(id, referenceNumber, fromAccountId, toAccountId, amount, description, status,
                createdAt, successAt, idempotencyKey, ledgerEntries);
    }

    // ---------------------------------------------------------------- state changes

    /**
     * Money has moved: record the sender's DEBIT and the recipient's CREDIT with their balances after,
     * and mark the transfer SUCCESS.
     */
    public Transaction completeTransfer(BigDecimal fromBalanceAfter, BigDecimal toBalanceAfter, Instant now) {
        requirePending();
        if (isCashDeposit()) {
            throw new IllegalStateException("A cash deposit has no sender: use completeCashDeposit");
        }
        return completed(List.of(
                LedgerEntry.debit(fromAccountId, amount, fromBalanceAfter, now),
                LedgerEntry.credit(toAccountId, amount, toBalanceAfter, now)), now);
    }

    /** Money has arrived: record the recipient's CREDIT with the balance after, and mark it SUCCESS. */
    public Transaction completeCashDeposit(BigDecimal toBalanceAfter, Instant now) {
        requirePending();
        if (!isCashDeposit()) {
            throw new IllegalStateException("A transfer needs both balances: use completeTransfer");
        }
        return completed(List.of(LedgerEntry.credit(toAccountId, amount, toBalanceAfter, now)), now);
    }

    private Transaction completed(List<LedgerEntry> entries, Instant now) {
        return new Transaction(id, referenceNumber, fromAccountId, toAccountId, amount, description,
                TransactionStatus.SUCCESS, createdAt, now, idempotencyKey, entries);
    }

    private void requirePending() {
        if (status != TransactionStatus.PENDING) {
            throw new IllegalStateException("Only a PENDING transaction can be completed, this one is " + status);
        }
    }

    // ---------------------------------------------------------------- questions

    public boolean isCashDeposit() {
        return fromAccountId == null;
    }

    /** True if the money left this account (never true for a cash deposit). Used by the idempotency check. */
    public boolean isSentFrom(Long accountId) {
        return fromAccountId != null && fromAccountId.equals(accountId);
    }

    /** True if the account sent or received this money. */
    public boolean involves(Long accountId) {
        return isSentFrom(accountId) || toAccountId.equals(accountId);
    }

    // ---------------------------------------------------------------- getters (no setters)

    public Long getId() { return id; }
    public String getReferenceNumber() { return referenceNumber; }
    public Long getFromAccountId() { return fromAccountId; }
    public Long getToAccountId() { return toAccountId; }
    public BigDecimal getAmount() { return amount; }
    public String getDescription() { return description; }
    public TransactionStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getSuccessAt() { return successAt; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public List<LedgerEntry> getLedgerEntries() { return ledgerEntries; }

    private static void requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
    }
}
