package com.Banking_app.transaction.application.service;

import com.Banking_app.transaction.application.port.out.CurrencyConversionPort;
import com.Banking_app.transaction.domain.models.transaction.CurrencyConversion;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.HashMap;
import java.util.Map;

/**
 * Stands in for the fx module, with the same round rates as the test properties: sell USD 4.75, SGD 3.50,
 * JPY 0.0305; buy USD 4.60, SGD 3.40, JPY 0.0290. Rates can be changed to simulate the market moving between a
 * request and its retry. Unknown currencies are refused with fx's message.
 */
class FakeCurrencyConversions implements CurrencyConversionPort {

    private final Map<String, BigDecimal> sellRates = new HashMap<>(Map.of(
            "USD", new BigDecimal("4.75"),
            "SGD", new BigDecimal("3.50"),
            "JPY", new BigDecimal("0.0305")));
    private final Map<String, BigDecimal> buyRates = new HashMap<>(Map.of(
            "USD", new BigDecimal("4.60"),
            "SGD", new BigDecimal("3.40"),
            "JPY", new BigDecimal("0.0290")));
    int calls;

    void setSellRate(String currency, String rate) {
        sellRates.put(currency, new BigDecimal(rate));
    }

    void setBuyRate(String currency, String rate) {
        buyRates.put(currency, new BigDecimal(rate));
    }

    @Override
    public CurrencyConversion convertPayment(String currencyCode, BigDecimal amount) {
        return convert(sellRates, currencyCode, amount);
    }

    @Override
    public CurrencyConversion convertIncoming(String currencyCode, BigDecimal amount) {
        return convert(buyRates, currencyCode, amount);
    }

    private CurrencyConversion convert(Map<String, BigDecimal> rates, String currencyCode, BigDecimal amount) {
        calls++;
        BigDecimal rate = rates.get(currencyCode);
        if (rate == null) {
            throw new IllegalArgumentException("Currency " + currencyCode + " is not supported");
        }
        return new CurrencyConversion(Currency.getInstance(currencyCode), amount, rate,
                amount.multiply(rate).setScale(2, RoundingMode.HALF_UP));
    }
}
