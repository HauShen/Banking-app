package com.Banking_app.transaction.adapter.in.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * JSON body of POST /api/admin/remittances: money from abroad arrives for a MYR account.
 *
 * {@code amount} is in {@code currency}; the decimals per currency (JPY none) and which currencies are offered are
 * checked by the fx module. {@code @Digits} matches the NUMERIC(19,2) columns, like the transfer request.
 * {@code senderName} ends up in the transaction's description (VARCHAR(255)), hence the 100-character limit;
 * {@code idempotencyKey} fits its VARCHAR(100) column.
 */
public record RemittanceRequestBody(
        @NotBlank(message = "toAccountNumber is required")
        String toAccountNumber,

        @NotBlank(message = "currency is required")
        @Pattern(regexp = "^[A-Z]{3}$", message = "currency must be a valid 3-letter ISO code (e.g. USD)")
        String currency,

        @NotNull(message = "amount is required")
        @DecimalMin(value = "0.01", inclusive = true, message = "amount must be greater than 0")
        @Digits(integer = 17, fraction = 2,
                message = "amount can have at most 2 decimal places and 17 digits before the decimal point")
        BigDecimal amount,

        @NotBlank(message = "senderName is required")
        @Size(max = 100, message = "senderName can have at most 100 characters")
        String senderName,

        @NotBlank(message = "idempotencyKey is required")
        @Size(max = 100, message = "idempotencyKey can have at most 100 characters")
        String idempotencyKey) {
}
