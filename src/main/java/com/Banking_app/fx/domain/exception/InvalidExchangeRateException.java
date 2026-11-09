package com.Banking_app.fx.domain.exception;

/**
 * An exchange rate that makes no sense: missing or non-positive rates, too many decimals, a buy rate above the sell
 * rate, MYR against itself. Rates come from configuration, so this stops the application at startup with a clear
 * message instead of quoting nonsense later.
 */
public class InvalidExchangeRateException extends IllegalArgumentException {

    public InvalidExchangeRateException(String message) {
        super(message);
    }
}
