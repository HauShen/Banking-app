package com.Banking_app.transaction.adapter.in.web.dto;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Reply to POST /api/transfers. The first four fields are as before; the others were added with foreign-currency
 * transfers.
 *
 * @param amount           the MYR amount that moved
 * @param originalCurrency the currency the customer paid in, e.g. "USD"; null for a MYR transfer
 * @param originalAmount   the amount in that currency; null for a MYR transfer
 * @param exchangeRate     MYR per 1 unit, the bank's sell rate at the time; null for a MYR transfer
 */
public record TransferResponseBody(String transactionReference,
                                   String status,
                                   Instant createdAt,
                                   String message,
                                   BigDecimal amount,
                                   String originalCurrency,
                                   BigDecimal originalAmount,
                                   BigDecimal exchangeRate) {
}
