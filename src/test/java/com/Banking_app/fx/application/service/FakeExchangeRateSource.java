package com.Banking_app.fx.application.service;

import com.Banking_app.fx.application.port.out.ExchangeRateSourcePort;
import com.Banking_app.fx.domain.models.ExchangeRate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Currency;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** In-memory rates for the use-case tests. */
class FakeExchangeRateSource implements ExchangeRateSourcePort {

    static final Instant AS_OF = Instant.parse("2026-11-09T00:00:00Z");

    private final Map<Currency, ExchangeRate> rates = new LinkedHashMap<>();

    FakeExchangeRateSource with(String currency, String buy, String sell) {
        Currency c = Currency.getInstance(currency);
        rates.put(c, new ExchangeRate(c, new BigDecimal(buy), new BigDecimal(sell), AS_OF));
        return this;
    }

    @Override
    public Optional<ExchangeRate> findRate(Currency currency) {
        return Optional.ofNullable(rates.get(currency));
    }

    @Override
    public List<ExchangeRate> allRates() {
        return new ArrayList<>(rates.values());
    }
}
