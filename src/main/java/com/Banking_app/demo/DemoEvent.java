package com.Banking_app.demo;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

/**
 * One money movement in the demo story: becomes one row in {@code transactions} and one or two in {@code ledgers}
 * (step 4). Built only by {@link DemoStoryGenerator}; the constructor checks the same rules as the database's
 * CHECK constraints on {@code transactions}, so a broken event fails here and not halfway through the seeding.
 *
 * <p>The type is the demo package's own enum; its names match the transaction module's {@code TransactionType}
 * (DemoStoryGeneratorTest checks that), because they are written as text into {@code transactions.transaction_type}.
 *
 * @param at          {@code created_at} = {@code success_at} of the transaction and of its ledger rows
 * @param type        TRANSFER, CASH_DEPOSIT or FOREIGN_REMITTANCE
 * @param fromAccount sender's account number; set for a TRANSFER only (chk_transactions_sender_matches_type)
 * @param toAccount   receiver's account number
 * @param amount      MYR that moved, 2 decimals, more than 0
 * @param description {@code transactions.description}, at most 255 characters
 * @param fx          the foreign-currency part; set for a FOREIGN_REMITTANCE only
 */
public record DemoEvent(Instant at, Type type, String fromAccount, String toAccount, BigDecimal amount,
                        String description, Fx fx) {

    public enum Type { TRANSFER, CASH_DEPOSIT, FOREIGN_REMITTANCE }

    /**
     * Money received from abroad: {@code transactions.original_currency}, {@code original_amount} and
     * {@code exchange_rate}, all set together (chk_transactions_fx_all_or_nothing).
     *
     * @param currency       ISO code, e.g. "USD"
     * @param originalAmount amount in that currency, more than 0
     * @param rate           MYR per 1 unit: the bank's buy rate, as the remittance service uses it
     */
    public record Fx(String currency, BigDecimal originalAmount, BigDecimal rate) {
        public Fx {
            Objects.requireNonNull(currency, "currency");
            Objects.requireNonNull(originalAmount, "originalAmount");
            Objects.requireNonNull(rate, "rate");
            if (!currency.matches("[A-Z]{3}")) {
                throw new IllegalArgumentException("Currency must be a 3-letter code: " + currency);
            }
            if (originalAmount.signum() <= 0 || rate.signum() <= 0) {
                throw new IllegalArgumentException("Original amount and rate must be more than 0");
            }
        }
    }

    public DemoEvent {
        Objects.requireNonNull(at, "at");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(toAccount, "toAccount");
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(description, "description");
        if (amount.signum() <= 0) {
            throw new IllegalArgumentException("Amount must be more than 0: " + amount);
        }
        if (amount.stripTrailingZeros().scale() > 2) {
            throw new IllegalArgumentException("Amount can have at most 2 decimals: " + amount);
        }
        amount = amount.setScale(2);
        if ((type == Type.TRANSFER) != (fromAccount != null)) {
            throw new IllegalArgumentException("A sender is set exactly for transfers: " + type);
        }
        if (toAccount.equals(fromAccount)) {
            throw new IllegalArgumentException("Sender and receiver must differ: " + toAccount);
        }
        if ((type == Type.FOREIGN_REMITTANCE) != (fx != null)) {
            throw new IllegalArgumentException("The foreign-currency part is set exactly for remittances: " + type);
        }
        if (description.isBlank() || description.length() > 255) {
            throw new IllegalArgumentException("Description must be 1-255 characters");
        }
    }

    public static DemoEvent transfer(Instant at, String from, String to, BigDecimal amount, String description) {
        return new DemoEvent(at, Type.TRANSFER, from, to, amount, description, null);
    }

    public static DemoEvent cashDeposit(Instant at, String to, BigDecimal amount, String description) {
        return new DemoEvent(at, Type.CASH_DEPOSIT, null, to, amount, description, null);
    }

    public static DemoEvent remittance(Instant at, String to, BigDecimal myrAmount, Fx fx, String description) {
        return new DemoEvent(at, Type.FOREIGN_REMITTANCE, null, to, myrAmount, description, fx);
    }

    /** Same event at another time: used to give events that landed on the same second their own second. */
    DemoEvent withTime(Instant newTime) {
        return new DemoEvent(newTime, type, fromAccount, toAccount, amount, description, fx);
    }

    public boolean involves(String accountNumber) {
        return accountNumber.equals(toAccount) || accountNumber.equals(fromAccount);
    }
}
