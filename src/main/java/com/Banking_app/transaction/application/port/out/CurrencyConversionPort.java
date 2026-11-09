package com.Banking_app.transaction.application.port.out;

import com.Banking_app.transaction.domain.models.transaction.CurrencyConversion;

import java.math.BigDecimal;

/**
 * Converts a foreign amount to MYR at the bank's current rate. The fx module owns the rules (which rate, rounding,
 * decimals per currency, the RM 0.01 minimum), so a transfer books exactly what GET /api/fx/quote shows.
 *
 * Both methods throw an IllegalArgumentException (400) with a message the client can act on: currency not offered,
 * MYR, an amount with too many decimals for the currency, or a result below RM 0.01.
 */
public interface CurrencyConversionPort {

    /** The customer pays in a foreign currency (a transfer in USD): the bank sells it → sell rate. */
    CurrencyConversion convertPayment(String currencyCode, BigDecimal amount);

    /** Money arrives from abroad (a remittance): the bank buys the foreign currency → buy rate. */
    CurrencyConversion convertIncoming(String currencyCode, BigDecimal amount);
}
