package com.Banking_app.fx.application.port.in.result;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * A locked quote as the caller sees it: send {@code quoteId} with the transfer before {@code expiresAt}.
 *
 * @param exchangeRate the locked sell rate, MYR per 1 unit
 * @param myrAmount    what the transfer will move
 */
public record LockedQuoteView(UUID quoteId,
                              String currency,
                              BigDecimal amount,
                              BigDecimal exchangeRate,
                              BigDecimal myrAmount,
                              Instant expiresAt) {
}
