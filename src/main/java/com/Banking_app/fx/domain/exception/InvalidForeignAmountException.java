package com.Banking_app.fx.domain.exception;

/**
 * A foreign amount that can't be converted: missing, zero or negative, more decimals than the currency has, or a
 * result below RM 0.01. 400 via IllegalArgumentException.
 *
 * The transaction module converts through QuoteUseCase too (FxQuoteConversionAdapter), so a quote and the transfer it
 * previews refuse an amount the same way. "Amount must be greater than 0" matches the MYR transfer's wording.
 */
public class InvalidForeignAmountException extends IllegalArgumentException {

    private InvalidForeignAmountException(String message) {
        super(message);
    }

    public static InvalidForeignAmountException notPositive() {
        return new InvalidForeignAmountException("Amount must be greater than 0");
    }

    public static InvalidForeignAmountException tooManyDecimals(String currencyCode, int allowedDecimals) {
        return new InvalidForeignAmountException(allowedDecimals == 0
                ? currencyCode + " amounts cannot have decimal places"
                : currencyCode + " amounts can have at most " + allowedDecimals + " decimal places");
    }

    public static InvalidForeignAmountException belowOneSen() {
        return new InvalidForeignAmountException("The converted amount is less than RM 0.01");
    }
}
