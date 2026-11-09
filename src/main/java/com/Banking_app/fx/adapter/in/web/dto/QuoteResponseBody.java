package com.Banking_app.fx.adapter.in.web.dto;

import java.math.BigDecimal;
import java.time.Instant;

/** Reply to GET /api/fx/quote: {@code amount} {@code currency} is {@code myrAmount} MYR at {@code exchangeRate}. */
public record QuoteResponseBody(String currency,
                                BigDecimal amount,
                                String direction,
                                BigDecimal exchangeRate,
                                BigDecimal myrAmount,
                                Instant asOf) {
}
