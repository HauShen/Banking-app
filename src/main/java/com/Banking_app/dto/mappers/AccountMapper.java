package com.Banking_app.dto.mappers;

import com.Banking_app.dto.responseBodies.AccountResponseBody;
import com.Banking_app.models.Account;
import org.springframework.stereotype.Component;



public class AccountMapper {
    public static AccountResponseBody toResponse(Account account){
        return new AccountResponseBody(
                account.getAccountId(),
                account.getUser() != null ? account.getUser().getId() : null,
                account.getAccountNumber(),
                account.getAccountType(),
                account.getAccountStatus(),
                account.getCreatedAt()
        );
    }
}
