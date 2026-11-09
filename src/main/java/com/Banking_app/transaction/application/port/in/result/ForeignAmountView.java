package com.Banking_app.transaction.application.port.in.result;

import java.math.BigDecimal;

/**
 * What a foreign-currency transaction was in its own currency, and the rate it was booked at. Null on results for
 * MYR transactions.
 *
 * @param currency     ISO 4217 code, e.g. "USD"
 * @param amount       in that currency
 * @param exchangeRate MYR per 1 unit of the currency
 */
public record ForeignAmountView(String currency, BigDecimal amount, BigDecimal exchangeRate) {
}
