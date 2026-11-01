package com.Banking_app.account.adapter.out.persistence.jparepositories;

import com.Banking_app.account.adapter.out.persistence.entities.AccountJpaEntity;
import com.Banking_app.account.domain.enums.AccountStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AccountJpaRepository extends JpaRepository<AccountJpaEntity, Long> {
    boolean existsByAccountNumber(String accountNumber);

    Optional<AccountJpaEntity> findByAccountNumber(String accountNumber);

    List<AccountJpaEntity> findAllByUserId(String userId);

    List<AccountJpaEntity> findAllByAccountStatus(AccountStatus accountStatus);

    /** SELECT ... FOR UPDATE: the row stays locked until the transaction commits or rolls back. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from AccountJpaEntity a where a.accountId = :accountId")
    Optional<AccountJpaEntity> findByIdForUpdate(@Param("accountId") Long accountId);

    /** Same lock, by account number. Used by the legacy TransactionServiceImpl.transfer. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from AccountJpaEntity a where a.accountNumber = :accountNumber")
    Optional<AccountJpaEntity> findByAccountNumberForUpdate(@Param("accountNumber") String accountNumber);
}
