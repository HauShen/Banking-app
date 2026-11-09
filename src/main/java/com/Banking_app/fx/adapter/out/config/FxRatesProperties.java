package com.Banking_app.fx.adapter.out.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Fixed exchange rates from configuration, MYR per 1 unit of the foreign currency:
 *
 * <pre>
 * fx.rates.USD.buy=4.6000
 * fx.rates.USD.sell=4.7500
 * </pre>
 *
 * The currencies listed here are the ones the bank offers. Keys are ISO 4217 codes (any case: environment variables
 * such as FX_RATES_USD_BUY arrive in lower case and are upper-cased by ConfiguredExchangeRateSource).
 */
@ConfigurationProperties(prefix = "fx")
public record FxRatesProperties(Map<String, Rate> rates) {

    public FxRatesProperties {
        rates = rates == null ? Map.of() : Map.copyOf(rates);
    }

    /** @param buy the bank buys at this rate, @param sell the bank sells at this rate (buy &lt;= sell) */
    public record Rate(BigDecimal buy, BigDecimal sell) {
    }
}
