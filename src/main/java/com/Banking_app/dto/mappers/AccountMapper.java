package com.Banking_app.dto.mappers;

import com.Banking_app.dto.responseBodies.AccountResponseBody;
import com.Banking_app.account.adapter.out.persistence.entities.AccountJpaEntity;


public class AccountMapper {
    public static AccountResponseBody toResponse(AccountJpaEntity account){
        return new AccountResponseBody(
                account.getAccountId(),
                account.getUserId(),
                account.getAccountNumber(),
                account.getAccountType(),
                account.getAccountStatus(),
                account.getAccountCurrency(),
                account.getCurrentBalance(),
                account.getCreatedAt()
        );
    }
}
