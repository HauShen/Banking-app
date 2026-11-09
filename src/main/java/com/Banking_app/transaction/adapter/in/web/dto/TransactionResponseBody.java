package com.Banking_app.transaction.adapter.in.web.dto;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * One transaction in GET /api/transfers/{reference} and in the history pages. The old fields are unchanged;
 * {@code type} and the three original-amount fields were added with foreign-currency transfers.
 *
 * @param type              TRANSFER, CASH_DEPOSIT or FOREIGN_REMITTANCE
 * @param fromAccountNumber "Cash Deposit Machine" for a top-up
 * @param amount            the MYR amount that moved
 * @param originalCurrency  the foreign currency, e.g. "USD"; null for a MYR transaction
 * @param originalAmount    the amount in that currency; null for a MYR transaction
 * @param exchangeRate      MYR per 1 unit it was booked at; null for a MYR transaction
 * @param postedAt          when the transaction succeeded (success_at)
 */
public record TransactionResponseBody(String reference,
                                      String type,
                                      String fromAccountNumber,
                                      String toAccountNumber,
                                      BigDecimal amount,
                                      String currency,
                                      String originalCurrency,
                                      BigDecimal originalAmount,
                                      BigDecimal exchangeRate,
                                      String status,
                                      String description,
                                      Instant createdAt,
                                      Instant postedAt) {
}
