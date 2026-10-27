package com.Banking_app.account.application.port.in.usecase;

import com.Banking_app.account.domain.models.account.Account;

public interface GetAccountUseCase {
    Account getById(Long accountId);

    Account getByAccountNumber(String accountNumber);
}
