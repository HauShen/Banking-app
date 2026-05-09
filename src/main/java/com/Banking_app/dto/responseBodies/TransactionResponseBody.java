package com.Banking_app.dto.responseBodies;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
@Getter
@Setter
public class TransactionResponseBody {
    private String reference;
    private String fromAccountNumber;
    private String toAccountNumber;
    private String toAccountUserName;
    private BigDecimal amount;
    private String currency;
    private String status;
    private String description;
    private Instant createdAt;
    private Instant postedAt;
    public TransactionResponseBody(String reference, String fromAccountNumber, String toAccountNumber,String toAccountUserName, BigDecimal amount, String currency, String status, String description, Instant createdAt, Instant postedAt){
        this.reference = reference;
        this.fromAccountNumber = fromAccountNumber;
        this.toAccountNumber = toAccountNumber;
        this.toAccountUserName = toAccountUserName;
        this.amount = amount;
        this.currency = currency;
        this.status = status;
        this.description = description;
        this.createdAt = createdAt;
        this.postedAt = postedAt;
    }
    public TransactionResponseBody(){}
}
