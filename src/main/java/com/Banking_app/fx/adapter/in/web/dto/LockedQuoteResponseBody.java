package com.Banking_app.fx.adapter.in.web.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Reply to POST /api/fx/quotes. Send {@code quoteId} with the transfer before {@code expiresAt} to pay at
 * {@code exchangeRate}; the transfer then moves {@code myrAmount}.
 */
public record LockedQuoteResponseBody(UUID quoteId,
                                      String currency,
                                      BigDecimal amount,
                                      BigDecimal exchangeRate,
                                      BigDecimal myrAmount,
                                      Instant expiresAt) {
}
