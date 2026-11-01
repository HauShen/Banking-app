package com.Banking_app.account.application.port.out;

import com.Banking_app.account.domain.enums.AccountStatus;
import com.Banking_app.account.domain.models.account.Account;

import java.util.List;
import java.util.Optional;

public interface LoadAccountPort {
    Optional<Account> findById(Long accountId);

    /**
     * Loads the account AND locks its row (SELECT ... FOR UPDATE) until the current transaction ends.
     * Use it before changing an account, so two requests can't both read the old balance and
     * overwrite each other. Must be called inside a @Transactional method.
     */
    Optional<Account> loadForUpdate(Long accountId);

    Optional<Account> findByAccountNumber(String accountNumber);

    List<Account> findAll();

    List<Account> findAllByStatus(AccountStatus status);

    List<Account> findAllByOwnerId(String ownerId);

    boolean existsByAccountNumber(String accountNumber);
}
