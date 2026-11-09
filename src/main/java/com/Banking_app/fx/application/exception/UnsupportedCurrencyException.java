package com.Banking_app.fx.application.exception;

/**
 * The bank has no rate for this currency (the fx module decides which currencies are offered: those with a rate),
 * or the code is MYR, which needs no conversion. 400 via IllegalArgumentException. The transaction module converts
 * through QuoteUseCase too, so a transfer refuses a currency with the same message as a quote.
 */
public class UnsupportedCurrencyException extends IllegalArgumentException {

    private UnsupportedCurrencyException(String message) {
        super(message);
    }

    public static UnsupportedCurrencyException notSupported(String currencyCode) {
        return new UnsupportedCurrencyException("Currency " + currencyCode + " is not supported");
    }

    public static UnsupportedCurrencyException myr() {
        return new UnsupportedCurrencyException("MYR needs no conversion");
    }
}
