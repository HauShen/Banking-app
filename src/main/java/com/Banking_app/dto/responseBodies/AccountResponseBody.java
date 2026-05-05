package com.Banking_app.dto.responseBodies;

import com.Banking_app.models.enums.AccountCurrency;
import com.Banking_app.models.enums.AccountStatus;
import com.Banking_app.models.enums.AccountType;
import lombok.Data;

import java.time.Instant;

@Data
public class AccountResponseBody {
    private Long accountId;
    private String userId;
    private String accountNumber;
    private AccountType accountType;
    private AccountStatus accountStatus;
    private AccountCurrency accountCurrency;
    private Instant createdAt;
    public AccountResponseBody(Long accountId, String userId, String accountNumber, AccountType accountType, AccountStatus accountStatus,AccountCurrency accountCurrency, Instant createdAt){
        this.accountId = accountId;
        this.userId = userId;
        this.accountNumber = accountNumber;
        this.accountType = accountType;
        this.accountStatus = accountStatus;
        this.accountCurrency =accountCurrency;
        this.createdAt = createdAt;
    }
    public AccountResponseBody(){}
}
