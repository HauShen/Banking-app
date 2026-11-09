package com.Banking_app.transaction.domain.models.transaction;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Objects;

/**
 * A foreign-currency amount converted to MYR, as it was booked. Accounts are MYR only, so a transaction in USD stores
 * the MYR amount that moved plus this record of the original currency, amount and rate.
 *
 * The conversion itself (which rate, rounding, decimals per currency, the RM 0.01 minimum) is the fx module's job:
 * the transaction module gets it through CurrencyConversionPort, the same quote the customer sees at GET /api/fx/quote.
 * This record only checks that the values can be stored as they are (the transactions table: amounts NUMERIC(19,2),
 * rate NUMERIC(19,8)), so nothing is silently rounded by the database.
 *
 * Value object: immutable, compared by value, no identity.
 *
 * @param originalAmount in the original currency; read back from the database it has 2 decimals (JPY 10000.00)
 * @param exchangeRate   MYR per 1 unit of the original currency (e.g. 4.75 for USD, 0.0305 for JPY)
 * @param myrAmount      the amount that actually moved, also the transaction's amount
 */
public record CurrencyConversion(Currency originalCurrency,
                                 BigDecimal originalAmount,
                                 BigDecimal exchangeRate,
                                 BigDecimal myrAmount) {

    public static final Currency MYR = Currency.getInstance("MYR");

    static final int MAX_RATE_DECIMALS = 8;
    static final int MAX_AMOUNT_DECIMALS = 2;

    public CurrencyConversion {
        Objects.requireNonNull(originalCurrency, "originalCurrency");
        Objects.requireNonNull(originalAmount, "originalAmount");
        Objects.requireNonNull(exchangeRate, "exchangeRate");
        Objects.requireNonNull(myrAmount, "myrAmount");
        if (originalCurrency.equals(MYR)) {
            throw new IllegalArgumentException("A currency conversion needs a foreign currency, not MYR");
        }
        if (originalAmount.signum() <= 0 || exchangeRate.signum() <= 0 || myrAmount.signum() <= 0) {
            throw new IllegalArgumentException("A currency conversion needs positive amounts and rate");
        }
        if (originalAmount.scale() > MAX_AMOUNT_DECIMALS || myrAmount.scale() > MAX_AMOUNT_DECIMALS
                || exchangeRate.scale() > MAX_RATE_DECIMALS) {
            throw new IllegalArgumentException(
                    "A currency conversion stores amounts with at most 2 decimals and the rate with at most 8");
        }
    }
}
