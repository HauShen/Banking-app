package com.Banking_app.transaction.application.port.out.event;

import com.Banking_app.transaction.domain.models.transaction.CurrencyConversion;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * The original foreign amount of a transaction that was converted to MYR, as it appears in an event.
 *
 * @param currency     ISO code, e.g. USD
 * @param exchangeRate MYR per 1 unit of {@code currency}
 */
public record ForeignAmount(String currency, BigDecimal amount, BigDecimal exchangeRate) {

    /** The transaction's conversion, or null for a plain MYR transaction. */
    static ForeignAmount of(Optional<CurrencyConversion> conversion) {
        return conversion
                .map(c -> new ForeignAmount(c.originalCurrency().getCurrencyCode(), c.originalAmount(), c.exchangeRate()))
                .orElse(null);
    }
}
