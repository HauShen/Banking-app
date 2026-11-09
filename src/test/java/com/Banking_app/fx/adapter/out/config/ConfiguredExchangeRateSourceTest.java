package com.Banking_app.fx.adapter.out.config;

import com.Banking_app.fx.domain.exception.InvalidExchangeRateException;
import com.Banking_app.fx.domain.models.ExchangeRate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Currency;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Plain unit test: properties in, rates out (FxEndpointTest covers the real Spring binding). */
class ConfiguredExchangeRateSourceTest {

    private static final Instant NOW = Instant.parse("2026-11-09T08:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    private static FxRatesProperties.Rate rate(String buy, String sell) {
        return new FxRatesProperties.Rate(new BigDecimal(buy), new BigDecimal(sell));
    }

    private static ConfiguredExchangeRateSource source(Map<String, FxRatesProperties.Rate> rates) {
        return new ConfiguredExchangeRateSource(new FxRatesProperties(rates), CLOCK);
    }

    @Test
    @DisplayName("each configured currency becomes a rate, stamped with the time it was loaded")
    void loadsRates() {
        ConfiguredExchangeRateSource source = source(Map.of("USD", rate("4.60", "4.75"), "JPY", rate("0.0290", "0.0305")));

        ExchangeRate usd = source.findRate(Currency.getInstance("USD")).orElseThrow();
        assertThat(usd.buyRate()).isEqualByComparingTo("4.60");
        assertThat(usd.sellRate()).isEqualByComparingTo("4.75");
        assertThat(usd.asOf()).isEqualTo(NOW);
        assertThat(source.allRates()).hasSize(2);
    }

    @Test
    @DisplayName("a currency that isn't configured is not found")
    void notConfigured() {
        assertThat(source(Map.of("USD", rate("4.60", "4.75"))).findRate(Currency.getInstance("SGD"))).isEmpty();
    }

    @Test
    @DisplayName("keys in lower case (e.g. from FX_RATES_USD_BUY) are upper-cased")
    void lowerCaseKey() {
        assertThat(source(Map.of("usd", rate("4.60", "4.75"))).findRate(Currency.getInstance("USD"))).isPresent();
    }

    @Test
    @DisplayName("no rates configured → nothing is offered")
    void noRates() {
        assertThat(new ConfiguredExchangeRateSource(new FxRatesProperties(null), CLOCK).allRates()).isEmpty();
    }

    @Test
    @DisplayName("startup fails on an unknown code, a currency configured twice, or a rate that makes no sense")
    void failsFast() {
        assertThatThrownBy(() -> source(Map.of("XYZ", rate("1", "1"))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("fx.rates.XYZ: not an ISO 4217 currency code");
        assertThatThrownBy(() -> source(Map.of("usd", rate("4.60", "4.75"), "USD", rate("4.60", "4.75"))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("fx.rates: USD is configured twice");
        assertThatThrownBy(() -> source(Map.of("USD", rate("4.80", "4.75"))))
                .isInstanceOf(InvalidExchangeRateException.class)
                .hasMessage("USD: the buy rate (4.80) must not be above the sell rate (4.75)");
        assertThatThrownBy(() -> source(Map.of("MYR", rate("1", "1"))))
                .isInstanceOf(InvalidExchangeRateException.class);
    }
}
