package com.Banking_app.fx.application.port.in.result;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * One currency's rates, as other modules and the web adapter see them (no domain type leaves the module).
 *
 * @param currency ISO 4217 code, e.g. "USD"
 * @param buyRate  MYR per 1 unit when the bank buys it (the customer receives foreign money)
 * @param sellRate MYR per 1 unit when the bank sells it (the customer pays in that currency)
 */
public record ExchangeRateView(String currency, BigDecimal buyRate, BigDecimal sellRate, Instant asOf) {
}
