package com.Banking_app.fx.adapter.in.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;

/**
 * JSON body of POST /api/fx/quotes: lock the sell rate for paying {@code amount} in {@code currency}.
 * Same field rules as the transfer request; decimals per currency (JPY none) are checked by the domain.
 */
public record LockQuoteRequestBody(
        @NotBlank(message = "currency is required")
        @Pattern(regexp = "^[A-Z]{3}$", message = "currency must be a valid 3-letter ISO code (e.g. USD)")
        String currency,

        @NotNull(message = "amount is required")
        @DecimalMin(value = "0.01", inclusive = true, message = "amount must be greater than 0")
        @Digits(integer = 17, fraction = 2,
                message = "amount can have at most 2 decimal places and 17 digits before the decimal point")
        BigDecimal amount) {
}
