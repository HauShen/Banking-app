package com.Banking_app.account.adapter.out.persistence.jparepositories;

import com.Banking_app.account.adapter.out.persistence.entities.AccountJpaEntity;
import com.Banking_app.account.domain.enums.AccountStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AccountJpaRepository extends JpaRepository<AccountJpaEntity, Long> {
    boolean existsByAccountNumber(String accountNumber);

    Optional<AccountJpaEntity> findByAccountNumber(String accountNumber);

    List<AccountJpaEntity> findAllByUserId(String userId);

    List<AccountJpaEntity> findAllByAccountStatus(AccountStatus accountStatus);

    /**
     * @deprecated Old AccountServiceImpl only. Fails when 2+ accounts share a status.
     * Removed in step 10 together with AccountServiceImpl; use findAllByAccountStatus.
     */
    @Deprecated
    Optional<AccountJpaEntity> findByAccountStatus(AccountStatus accountStatus);
}
