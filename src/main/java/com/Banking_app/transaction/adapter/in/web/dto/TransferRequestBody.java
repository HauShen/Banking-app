package com.Banking_app.transaction.adapter.in.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;

/**
 * JSON body of POST /api/transfers. Same fields, validation and messages as the old dto.requestBodies.TransferRequestBody
 * — including the copy-paste message on toAccountNumber ("fromAccountNumber is required"): the refactor changes no
 * behaviour, fix it later on its own.
 *
 * {@code currency} is validated but not used, as before (plan, Phase 4 item 16).
 *
 * {@code @Digits} matches the NUMERIC(19,2) columns: at most 2 decimals as written (for a BigDecimal, 10.500 counts
 * as 3) and 17 digits before the point. Bigger amounts used to fail in the database with a 500.
 */
public record TransferRequestBody(
        @NotNull(message = "fromAccountNumber is required")
        String fromAccountNumber,

        @NotNull(message = "fromAccountNumber is required")
        String toAccountNumber,

        @NotNull(message = "amount is required")
        @DecimalMin(value = "0.01", inclusive = true, message = "amount must be greater than 0")
        @Digits(integer = 17, fraction = 2,
                message = "amount can have at most 2 decimal places and 17 digits before the decimal point")
        BigDecimal amount,

        @Pattern(regexp = "^[A-Z]{3}$", message = "currency must be a valid 3-letter ISO code (e.g. MYR)")
        String currency,

        @NotBlank(message = "description is required")
        String description,

        @NotBlank(message = "idempotencyKey is required")
        String idempotencyKey) {
}
