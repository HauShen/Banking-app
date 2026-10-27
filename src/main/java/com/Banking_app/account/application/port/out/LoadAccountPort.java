package com.Banking_app.account.application.port.out;

import com.Banking_app.account.domain.enums.AccountStatus;
import com.Banking_app.account.domain.models.account.Account;

import java.util.List;
import java.util.Optional;

public interface LoadAccountPort {
    Optional<Account> findById(Long accountId);

    Optional<Account> findByAccountNumber(String accountNumber);

    List<Account> findAll();

    List<Account> findAllByStatus(AccountStatus status);

    List<Account> findAllByOwnerId(String ownerId);

    boolean existsByAccountNumber(String accountNumber);
}
