package com.Banking_app.fx.adapter.out.config;

import com.Banking_app.fx.application.port.out.ExchangeRateSourcePort;
import com.Banking_app.fx.domain.models.ExchangeRate;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.Currency;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Outbound adapter: the rates in {@code fx.rates.*} (see FxRatesProperties), fixed while the application runs.
 *
 * Everything is checked once, when the application starts: an unknown code or a rate that makes no sense (zero, a
 * buy rate above the sell rate, ...) stops startup with a message naming the currency. {@code asOf} is the time the
 * rates were loaded.
 */
@Component
public class ConfiguredExchangeRateSource implements ExchangeRateSourcePort {

    private final Map<Currency, ExchangeRate> rates;

    public ConfiguredExchangeRateSource(FxRatesProperties properties, Clock clock) {
        Instant loadedAt = clock.instant();
        Map<Currency, ExchangeRate> byCurrency = new HashMap<>();
        properties.rates().forEach((key, rate) -> {
            Currency currency = currencyOf(key);
            ExchangeRate exchangeRate = new ExchangeRate(currency, rate.buy(), rate.sell(), loadedAt);
            if (byCurrency.put(currency, exchangeRate) != null) {
                throw new IllegalStateException("fx.rates: " + currency.getCurrencyCode() + " is configured twice");
            }
        });
        this.rates = Map.copyOf(byCurrency);
    }

    private static Currency currencyOf(String key) {
        try {
            return Currency.getInstance(key.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException unknownCode) {
            throw new IllegalStateException("fx.rates." + key + ": not an ISO 4217 currency code");
        }
    }

    @Override
    public Optional<ExchangeRate> findRate(Currency currency) {
        return Optional.ofNullable(rates.get(currency));
    }

    @Override
    public List<ExchangeRate> allRates() {
        return List.copyOf(rates.values());
    }
}
