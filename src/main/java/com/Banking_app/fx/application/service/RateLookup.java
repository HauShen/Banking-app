package com.Banking_app.fx.application.service;

import com.Banking_app.fx.application.exception.UnsupportedCurrencyException;
import com.Banking_app.fx.application.port.out.ExchangeRateSourcePort;
import com.Banking_app.fx.domain.models.ExchangeRate;

import java.util.Currency;

/** Currency code → the bank's rate, shared by both services. Codes are exact ISO codes: "usd" is not "USD". */
final class RateLookup {

    private RateLookup() {
    }

    static ExchangeRate require(ExchangeRateSourcePort source, String currencyCode) {
        if (currencyCode == null || currencyCode.isBlank()) {
            throw UnsupportedCurrencyException.notSupported(String.valueOf(currencyCode));
        }
        if (currencyCode.equals(ExchangeRate.MYR.getCurrencyCode())) {
            throw UnsupportedCurrencyException.myr();
        }
        Currency currency;
        try {
            currency = Currency.getInstance(currencyCode);
        } catch (IllegalArgumentException unknownCode) {
            throw UnsupportedCurrencyException.notSupported(currencyCode);
        }
        return source.findRate(currency)
                .orElseThrow(() -> UnsupportedCurrencyException.notSupported(currencyCode));
    }
}
