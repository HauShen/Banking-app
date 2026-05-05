package com.Banking_app.dto.responseBodies;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.OffsetDateTime;
@Data
@Getter
@Setter
public class TransferResponseBody {
    private String transactionReference;
    private String status;
    private Instant createdAt;
    private String message;
    public TransferResponseBody(String transactionReference, String status, Instant createdAt, String message){
        this.transactionReference = transactionReference;
        this.status = status;
        this. createdAt = createdAt;
        this. message = message;
    }
    public TransferResponseBody(){}
}
