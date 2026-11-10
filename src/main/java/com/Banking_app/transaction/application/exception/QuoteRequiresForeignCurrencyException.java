package com.Banking_app.transaction.application.exception;

/** A locked quote fixes an exchange rate, so it makes no sense for a MYR transfer. 400 via IllegalArgumentException. */
public class QuoteRequiresForeignCurrencyException extends IllegalArgumentException {

    public QuoteRequiresForeignCurrencyException() {
        super("quoteId can only be used with a foreign currency");
    }
}
