package com.Banking_app.dto.responseBodies;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
@Data
@Builder
public class BalanceDTO {
    private Long accountId;
    private BigDecimal availableBalance;
    private BigDecimal currentBalance;
    private String currency;
    public BalanceDTO(Long accountId, BigDecimal availableBalance, BigDecimal currentBalance, String currency){
        this.accountId = accountId;
        this.availableBalance = availableBalance;
        this.currentBalance = currentBalance;
        this.currency = currency;
    }
    public BalanceDTO(){}
}
