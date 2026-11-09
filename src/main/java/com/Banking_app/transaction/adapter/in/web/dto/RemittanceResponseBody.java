package com.Banking_app.transaction.adapter.in.web.dto;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Reply to POST /api/admin/remittances.
 *
 * @param amount           the MYR amount credited
 * @param originalCurrency the currency it was sent in, e.g. "USD"
 * @param originalAmount   the amount in that currency
 * @param exchangeRate     MYR per 1 unit, the bank's buy rate at the time
 */
public record RemittanceResponseBody(String transactionReference,
                                     String status,
                                     Instant createdAt,
                                     String message,
                                     String toAccountNumber,
                                     BigDecimal amount,
                                     String originalCurrency,
                                     BigDecimal originalAmount,
                                     BigDecimal exchangeRate) {
}
