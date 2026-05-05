package com.Banking_app.dto.requestBodies;

import com.Banking_app.models.Account;
import jakarta.validation.constraints.DecimalMin;
import lombok.Data;
import lombok.Builder;
import java.math.BigDecimal;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
@Data
@Builder
public class TransferRequestBody {
    @NotNull(message = "fromAccountId is required")
    private Long fromAccountId;
    @NotNull(message = "toAccountId is required")
    private Long toAccountId;
    @NotNull(message = "amount is required")
    @DecimalMin(value = "0.01", inclusive = true, message = "amount must be greater than 0")
    private BigDecimal amount;
    @Pattern(regexp = "^[A-Z]{3}$", message = "currency must be a valid 3-letter ISO code (e.g. MYR)")
    private BigDecimal currency;
    @NotBlank(message = "description is required")
    private String description;
    @NotBlank(message = "idempotencyKey is required")
    private String idempotencyKey;
    public TransferRequestBody(Long fromAccountId, Long toAccountId, BigDecimal amount, BigDecimal currency, String description, String idempotencyKey){
        this.fromAccountId = fromAccountId;
        this.toAccountId = toAccountId;
        this.amount = amount;
        this.currency = currency;
        this.description = description;
        this.idempotencyKey = idempotencyKey;
    }
    public TransferRequestBody(){}
}
