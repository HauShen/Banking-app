package com.Banking_app.repositories;

import com.Banking_app.dto.requestBodies.AccountRequestBody;
import com.Banking_app.dto.responseBodies.AccountResponseBody;
import com.Banking_app.models.Account;
import com.Banking_app.models.enums.AccountStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account,Long> {
    boolean existsByAccountNumber(String accountNumber);
    Optional<Account> findByAccountNumber(String accountNumber);
    Optional<Account> findByAccountStatus(AccountStatus accountStatus);

}
