package com.Banking_app.transaction.domain.exception;

/**
 * A foreign-currency amount that can't be converted to MYR: unsupported currency, too many decimals, bad rate, or a
 * result below RM 0.01. 400 via IllegalArgumentException, with a message the client can act on.
 */
public class InvalidCurrencyConversionException extends IllegalArgumentException {

    private InvalidCurrencyConversionException(String message) {
        super(message);
    }

    public static InvalidCurrencyConversionException notSupported(String currencyCode) {
        return new InvalidCurrencyConversionException("Currency " + currencyCode + " is not supported");
    }

    public static InvalidCurrencyConversionException notForeign() {
        return new InvalidCurrencyConversionException("MYR needs no conversion");
    }

    public static InvalidCurrencyConversionException tooManyDecimals(String currencyCode, int allowedDecimals) {
        return new InvalidCurrencyConversionException(allowedDecimals == 0
                ? currencyCode + " amounts cannot have decimal places"
                : currencyCode + " amounts can have at most " + allowedDecimals + " decimal places");
    }

    public static InvalidCurrencyConversionException invalidRate() {
        return new InvalidCurrencyConversionException(
                "Exchange rate must be greater than 0 with at most 8 decimal places");
    }

    public static InvalidCurrencyConversionException belowOneSen() {
        return new InvalidCurrencyConversionException("The converted amount is less than RM 0.01");
    }
}
