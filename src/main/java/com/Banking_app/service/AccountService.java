package com.Banking_app.service;

import com.Banking_app.dto.requestBodies.AccountRequestBody;
import com.Banking_app.dto.responseBodies.AccountResponseBody;
import com.Banking_app.models.enums.AccountStatus;

import java.util.List;

public interface AccountService {
    AccountResponseBody createAccountWithUserId(AccountRequestBody accountRequestBody);
    AccountResponseBody getAccountByAccountId(Long accountId);
    AccountResponseBody getByAccountNumber(String accountNumber);
    List<AccountResponseBody> getAllAccounts();
    List<AccountResponseBody> getAccountsByStatus(AccountStatus status);
    AccountResponseBody updateAccountStatus(Long accountId,AccountStatus status);
    void deleteAccount(Long accountId);
}
