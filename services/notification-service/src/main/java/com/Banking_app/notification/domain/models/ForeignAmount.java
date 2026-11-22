package com.Banking_app.notification.domain.models;

import java.math.BigDecimal;
import java.util.Objects;

/** The amount in the currency the money was sent or received in, shown next to the MYR amount, e.g. (USD 20.00). */
public record ForeignAmount(String currency, BigDecimal amount) {

    public ForeignAmount {
        Objects.requireNonNull(currency, "currency");
        Objects.requireNonNull(amount, "amount");
    }

    @Override
    public String toString() {
        return currency + " " + amount.toPlainString();
    }
}
