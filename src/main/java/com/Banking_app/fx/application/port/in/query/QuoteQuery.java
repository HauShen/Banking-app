package com.Banking_app.fx.application.port.in.query;

import com.Banking_app.fx.domain.enums.ConversionDirection;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * "How much MYR is this foreign amount?"
 *
 * @param currencyCode ISO 4217 code, e.g. "USD" (checked by the use case: the bank must offer it)
 * @param amount       in that currency (checked by the domain: positive, the currency's decimals)
 */
public record QuoteQuery(String currencyCode, BigDecimal amount, ConversionDirection direction) {

    public QuoteQuery {
        Objects.requireNonNull(direction, "direction");
    }
}
