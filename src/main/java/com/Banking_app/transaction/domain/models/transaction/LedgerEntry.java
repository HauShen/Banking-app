package com.Banking_app.transaction.domain.models.transaction;

import com.Banking_app.transaction.domain.enums.LedgerType;
import com.Banking_app.transaction.domain.exception.InvalidTransactionAmountException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

/**
 * One line in an account's statement: money out (DEBIT) or in (CREDIT), and the account's balance right after.
 * Only exists inside a {@link Transaction}, which creates it in completeTransfer / completeCashDeposit.
 *
 * The amount is always positive, for DEBIT too: the type says the direction (same as the rows stored today).
 * The account is a plain id, not an Account object: the account belongs to another module.
 *
 * @param id null until saved
 */
public record LedgerEntry(Long id,
                          Long accountId,
                          LedgerType type,
                          BigDecimal amount,
                          BigDecimal balanceAfter,
                          Instant postedAt) {

    public LedgerEntry {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(balanceAfter, "balanceAfter");
        Objects.requireNonNull(postedAt, "postedAt");
        if (amount == null || amount.signum() <= 0) {
            throw new InvalidTransactionAmountException();
        }
    }

    static LedgerEntry debit(Long accountId, BigDecimal amount, BigDecimal balanceAfter, Instant postedAt) {
        return new LedgerEntry(null, accountId, LedgerType.DEBIT, amount, balanceAfter, postedAt);
    }

    static LedgerEntry credit(Long accountId, BigDecimal amount, BigDecimal balanceAfter, Instant postedAt) {
        return new LedgerEntry(null, accountId, LedgerType.CREDIT, amount, balanceAfter, postedAt);
    }
}
