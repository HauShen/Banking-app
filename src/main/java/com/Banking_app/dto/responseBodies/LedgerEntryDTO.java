package com.Banking_app.dto.responseBodies;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
@Data
@Builder
public class LedgerEntryDTO {
    private Long accountId;
    private String entryType; // DEBIT or CREDIT
    private BigDecimal amount;
    private String currency;
    private String transactionReference;
    private OffsetDateTime postedAt;
    public LedgerEntryDTO(Long accountId, String entryType, BigDecimal amount, String currency, String transactionReference, OffsetDateTime postedAt){
        this.accountId = accountId;
        this.entryType = entryType;
        this.currency = currency;
        this.transactionReference = transactionReference;
        this.postedAt = postedAt;
    }
    public LedgerEntryDTO(){}
}
