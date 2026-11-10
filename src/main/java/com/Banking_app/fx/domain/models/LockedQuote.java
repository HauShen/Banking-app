package com.Banking_app.fx.domain.models;

import com.Banking_app.fx.domain.enums.ConversionDirection;
import com.Banking_app.fx.domain.exception.InvalidQuoteException;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Currency;
import java.util.Objects;
import java.util.UUID;

/**
 * A quote with its rate fixed for a short time: the customer locks the bank's SELL rate for an amount in a foreign
 * currency, then pays a transfer at exactly that rate (instead of the rate at the moment the transfer runs).
 *
 * Belongs to the user who locked it and is single-use ({@code usedAt} is set by {@link #consume} when a transfer
 * uses it).
 * The MYR amount is computed once, when locking, with the same rule as every conversion ({@link ExchangeRate#toMyr}).
 */
public final class LockedQuote {

    private final UUID id;
    private final String ownerId;
    private final Currency currency;
    private final BigDecimal amount;
    private final BigDecimal exchangeRate;
    private final BigDecimal myrAmount;
    private final Instant createdAt;
    private final Instant expiresAt;
    private final Instant usedAt;

    private LockedQuote(UUID id, String ownerId, Currency currency, BigDecimal amount, BigDecimal exchangeRate,
                        BigDecimal myrAmount, Instant createdAt, Instant expiresAt, Instant usedAt) {
        this.id = Objects.requireNonNull(id, "id");
        this.ownerId = Objects.requireNonNull(ownerId, "ownerId");
        this.currency = Objects.requireNonNull(currency, "currency");
        this.amount = Objects.requireNonNull(amount, "amount");
        this.exchangeRate = Objects.requireNonNull(exchangeRate, "exchangeRate");
        this.myrAmount = Objects.requireNonNull(myrAmount, "myrAmount");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt");
        if (!expiresAt.isAfter(createdAt)) {
            throw new IllegalArgumentException("A quote must expire after it was created");
        }
        this.usedAt = usedAt;
    }

    /**
     * Locks today's sell rate for {@code amount} in the rate's currency.
     *
     * @throws com.Banking_app.fx.domain.exception.InvalidForeignAmountException 400: not positive, too many decimals
     *                                                                         for the currency, below RM 0.01
     */
    public static LockedQuote lock(UUID id, String ownerId, ExchangeRate rate, BigDecimal amount, Instant now,
                                   Duration validity) {
        Objects.requireNonNull(rate, "rate");
        Objects.requireNonNull(validity, "validity");
        if (validity.isNegative() || validity.isZero()) {
            throw new IllegalArgumentException("A quote must be valid for a positive time");
        }
        BigDecimal myrAmount = rate.toMyr(amount, ConversionDirection.PAY);   // checks the amount
        return new LockedQuote(id, ownerId, rate.currency(), amount, rate.rateFor(ConversionDirection.PAY),
                myrAmount, now, now.plus(validity), null);
    }

    /** Rebuilds a stored quote (persistence adapter only). */
    public static LockedQuote rehydrate(UUID id, String ownerId, Currency currency, BigDecimal amount,
                                        BigDecimal exchangeRate, BigDecimal myrAmount, Instant createdAt,
                                        Instant expiresAt, Instant usedAt) {
        return new LockedQuote(id, ownerId, currency, amount, exchangeRate, myrAmount, createdAt, expiresAt, usedAt);
    }

    /** Expired from {@code expiresAt} on (a quote valid for 60 s is no longer valid at second 60). */
    public boolean isExpired(Instant now) {
        return !now.isBefore(expiresAt);
    }

    public boolean isUsed() {
        return usedAt != null;
    }

    /**
     * Uses the quote for a payment: the requester's payment of {@code amount} in {@code currencyCode}, now.
     * Returns the used quote ({@code usedAt} = now); this one is unchanged.
     *
     * Checked in this order, all 400: someone else's quote → "Quote not found" (as if it didn't exist); already used;
     * expired; a different currency or amount (compared by value: 100 = 100.00, and JPY comes back as 10000.00).
     */
    public LockedQuote consume(String requesterId, String currencyCode, BigDecimal paymentAmount, Instant now) {
        if (!ownerId.equals(requesterId)) {
            throw InvalidQuoteException.notFound();
        }
        if (isUsed()) {
            throw InvalidQuoteException.alreadyUsed();
        }
        if (isExpired(now)) {
            throw InvalidQuoteException.expired();
        }
        if (!currency.getCurrencyCode().equals(currencyCode)
                || paymentAmount == null || amount.compareTo(paymentAmount) != 0) {
            throw InvalidQuoteException.differentPayment();
        }
        return new LockedQuote(id, ownerId, currency, amount, exchangeRate, myrAmount, createdAt, expiresAt, now);
    }

    public UUID getId() {
        return id;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public Currency getCurrency() {
        return currency;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public BigDecimal getExchangeRate() {
        return exchangeRate;
    }

    public BigDecimal getMyrAmount() {
        return myrAmount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getUsedAt() {
        return usedAt;
    }
}
