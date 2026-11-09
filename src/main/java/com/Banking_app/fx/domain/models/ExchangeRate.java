package com.Banking_app.fx.domain.models;

import com.Banking_app.fx.domain.enums.ConversionDirection;
import com.Banking_app.fx.domain.exception.InvalidExchangeRateException;
import com.Banking_app.fx.domain.exception.InvalidForeignAmountException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Currency;
import java.util.Objects;

/**
 * The bank's rates for one foreign currency, in MYR per 1 unit of it (USD ≈ 4.7, JPY ≈ 0.03).
 *
 * The bank buys foreign money cheaper than it sells it, so {@code buyRate <= sellRate}; the difference is its margin.
 * Which rate applies depends on the direction (see {@link ConversionDirection}).
 *
 * Value object: immutable, compared by value. Every instance is valid, so a wrong configuration fails at startup.
 *
 * @param asOf when these rates were set (for fixed rates from configuration: when they were loaded)
 */
public record ExchangeRate(Currency currency, BigDecimal buyRate, BigDecimal sellRate, Instant asOf) {

    public static final Currency MYR = Currency.getInstance("MYR");

    /** The transactions table stores rates as NUMERIC(19,8) and amounts as NUMERIC(19,2). */
    static final int MAX_RATE_DECIMALS = 8;
    static final int MAX_AMOUNT_DECIMALS = 2;

    private static final BigDecimal ONE_SEN = new BigDecimal("0.01");

    public ExchangeRate {
        if (currency == null) {
            throw new InvalidExchangeRateException("An exchange rate needs a currency");
        }
        String code = currency.getCurrencyCode();
        if (currency.equals(MYR)) {
            throw new InvalidExchangeRateException("MYR has no exchange rate against itself");
        }
        int decimals = currency.getDefaultFractionDigits();
        if (decimals < 0 || decimals > MAX_AMOUNT_DECIMALS) {   // e.g. gold (XAU: -1) or KWD (3)
            throw new InvalidExchangeRateException(
                    code + " is not supported: only currencies with at most 2 decimal places can be converted");
        }
        requireValidRate(code, "buy", buyRate);
        requireValidRate(code, "sell", sellRate);
        if (buyRate.compareTo(sellRate) > 0) {
            throw new InvalidExchangeRateException(code + ": the buy rate (" + buyRate.toPlainString()
                    + ") must not be above the sell rate (" + sellRate.toPlainString() + ")");
        }
        Objects.requireNonNull(asOf, "asOf");
    }

    private static void requireValidRate(String code, String side, BigDecimal rate) {
        if (rate == null) {
            throw new InvalidExchangeRateException(code + ": the " + side + " rate is missing");
        }
        if (rate.signum() <= 0 || rate.scale() > MAX_RATE_DECIMALS) {
            throw new InvalidExchangeRateException(code + ": the " + side + " rate (" + rate.toPlainString()
                    + ") must be greater than 0 with at most " + MAX_RATE_DECIMALS + " decimal places");
        }
    }

    /** PAY → sell rate (the bank sells the foreign currency), RECEIVE → buy rate (the bank buys it). */
    public BigDecimal rateFor(ConversionDirection direction) {
        return switch (Objects.requireNonNull(direction, "direction")) {
            case PAY -> sellRate;
            case RECEIVE -> buyRate;
        };
    }

    /**
     * The MYR equivalent of {@code amount} in this currency, rounded HALF_UP to 2 decimals.
     *
     * The one place the bank's conversion rule lives: quotes and transactions both get here through QuoteUseCase.
     * The amount must be positive with at most the currency's usual decimals as written (USD 10.50 ok, 10.500 not;
     * JPY none at all), and the result at least RM 0.01.
     */
    public BigDecimal toMyr(BigDecimal amount, ConversionDirection direction) {
        if (amount == null || amount.signum() <= 0) {
            throw InvalidForeignAmountException.notPositive();
        }
        int allowedDecimals = currency.getDefaultFractionDigits();
        if (amount.scale() > allowedDecimals) {
            throw InvalidForeignAmountException.tooManyDecimals(currency.getCurrencyCode(), allowedDecimals);
        }
        BigDecimal myr = amount.multiply(rateFor(direction)).setScale(MAX_AMOUNT_DECIMALS, RoundingMode.HALF_UP);
        if (myr.compareTo(ONE_SEN) < 0) {
            throw InvalidForeignAmountException.belowOneSen();
        }
        return myr;
    }
}
