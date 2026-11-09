package com.Banking_app.transaction.domain.models.transaction;

import com.Banking_app.transaction.domain.exception.InvalidCurrencyConversionException;
import com.Banking_app.transaction.domain.exception.InvalidTransactionAmountException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.Objects;

/**
 * A foreign-currency amount converted to MYR at a given rate, as it happened. Accounts are MYR only, so a transaction
 * in USD stores the MYR amount that moved plus this record of the original currency, amount and rate.
 *
 * Value object: immutable, compared by value, no identity.
 *
 * @param exchangeRate MYR per 1 unit of the foreign currency (e.g. 4.75 for USD, 0.0305 for JPY), at most 8 decimals
 * @param myrAmount    the converted amount that actually moves, rounded HALF_UP to 2 decimals
 */
public record CurrencyConversion(Currency originalCurrency,
                                 BigDecimal originalAmount,
                                 BigDecimal exchangeRate,
                                 BigDecimal myrAmount) {

    public static final Currency MYR = Currency.getInstance("MYR");

    /** The transactions table stores rates as NUMERIC(19,8) and amounts as NUMERIC(19,2). */
    static final int MAX_RATE_DECIMALS = 8;
    static final int MAX_AMOUNT_DECIMALS = 2;

    private static final BigDecimal ONE_SEN = new BigDecimal("0.01");

    /** Basic sanity for every instance, also the ones rebuilt from the database (no recalculation there). */
    public CurrencyConversion {
        Objects.requireNonNull(originalCurrency, "originalCurrency");
        Objects.requireNonNull(originalAmount, "originalAmount");
        Objects.requireNonNull(exchangeRate, "exchangeRate");
        Objects.requireNonNull(myrAmount, "myrAmount");
        if (originalAmount.signum() <= 0 || exchangeRate.signum() <= 0 || myrAmount.signum() <= 0) {
            throw new IllegalArgumentException("A currency conversion needs positive amounts and rate");
        }
    }

    /**
     * Converts a foreign amount to MYR.
     *
     * @param currencyCode ISO 4217 code, e.g. "USD". Whether the bank offers it is decided by the fx module (a rate
     *                     exists); this only rejects what can't be represented: MYR itself, unknown codes, and
     *                     currencies with more than 2 decimals (the amount columns are NUMERIC(19,2)).
     * @param amount       in that currency, with at most its usual decimals as written (USD 10.50 ok, 10.500 not;
     *                     JPY none at all) — the same "as written" rule as MYR amounts
     * @param myrPerUnit   the rate to apply (the fx module picks buy or sell)
     */
    public static CurrencyConversion toMyr(String currencyCode, BigDecimal amount, BigDecimal myrPerUnit) {
        Currency currency = currencyOf(currencyCode);

        int allowedDecimals = currency.getDefaultFractionDigits();
        if (amount == null || amount.signum() <= 0) {
            throw new InvalidTransactionAmountException();
        }
        if (amount.scale() > allowedDecimals) {
            throw InvalidCurrencyConversionException.tooManyDecimals(currency.getCurrencyCode(), allowedDecimals);
        }
        if (myrPerUnit == null || myrPerUnit.signum() <= 0 || myrPerUnit.scale() > MAX_RATE_DECIMALS) {
            throw InvalidCurrencyConversionException.invalidRate();
        }

        BigDecimal myr = amount.multiply(myrPerUnit).setScale(MAX_AMOUNT_DECIMALS, RoundingMode.HALF_UP);
        if (myr.compareTo(ONE_SEN) < 0) {
            throw InvalidCurrencyConversionException.belowOneSen();
        }
        return new CurrencyConversion(currency, amount, myrPerUnit, myr);
    }

    private static Currency currencyOf(String currencyCode) {
        if (currencyCode == null || currencyCode.isBlank()) {
            throw InvalidCurrencyConversionException.notSupported(String.valueOf(currencyCode));
        }
        Currency currency;
        try {
            currency = Currency.getInstance(currencyCode);
        } catch (IllegalArgumentException unknownCode) {
            throw InvalidCurrencyConversionException.notSupported(currencyCode);
        }
        if (currency.equals(MYR)) {
            throw InvalidCurrencyConversionException.notForeign();
        }
        int decimals = currency.getDefaultFractionDigits();
        if (decimals < 0 || decimals > MAX_AMOUNT_DECIMALS) {   // e.g. gold (XAU: -1) or KWD (3)
            throw InvalidCurrencyConversionException.notSupported(currencyCode);
        }
        return currency;
    }
}
