package com.Banking_app.fx.adapter.in.web.dto;

import java.math.BigDecimal;
import java.time.Instant;

/** One entry of GET /api/fx/rates. Rates are MYR per 1 unit of the currency. */
public record ExchangeRateResponseBody(String currency,
                                       BigDecimal buyRate,
                                       BigDecimal sellRate,
                                       Instant asOf) {
}
