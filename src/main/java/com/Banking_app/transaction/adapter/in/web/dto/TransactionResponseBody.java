package com.Banking_app.transaction.adapter.in.web.dto;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * One transaction in GET /api/transfers/{reference} and in the history pages. Same JSON fields as before.
 *
 * @param fromAccountNumber "Cash Deposit Machine" for a top-up
 * @param postedAt          when the transaction succeeded (success_at)
 */
public record TransactionResponseBody(String reference,
                                      String fromAccountNumber,
                                      String toAccountNumber,
                                      BigDecimal amount,
                                      String currency,
                                      String status,
                                      String description,
                                      Instant createdAt,
                                      Instant postedAt) {
}
