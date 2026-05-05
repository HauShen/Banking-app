package com.Banking_app.dto.responseBodies;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
@Data
@Builder
public class TransactionResponseBody {
    private String reference;
    private Long fromAccountId;
    private Long toAccountId;
    private BigDecimal amount;
    private String currency;
    private String status;
    private String description;
    private Instant createdAt;
    private Instant postedAt;
    public TransactionResponseBody(String reference, Long fromAccountId, Long toAccountId, BigDecimal amount, String currency, String status, String description, Instant createdAt, Instant postedAt){
        this.reference = reference;
        this.fromAccountId = fromAccountId;
        this.amount = amount;
        this.currency = currency;
        this.status = status;
        this.description = description;
        this.createdAt = createdAt;
        this.postedAt = postedAt;
    }
    public TransactionResponseBody(){}
}
