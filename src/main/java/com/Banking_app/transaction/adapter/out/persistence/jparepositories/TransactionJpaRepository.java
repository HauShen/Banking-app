package com.Banking_app.transaction.adapter.out.persistence.jparepositories;

import com.Banking_app.transaction.adapter.out.persistence.entities.TransactionJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface TransactionJpaRepository extends JpaRepository<TransactionJpaEntity, Long> {

    Optional<TransactionJpaEntity> findByReferenceNumber(String referenceNumber);

    Optional<TransactionJpaEntity> findByIdempotencyKey(String idempotencyKey);

    /**
     * Transactions the account sent or received. Plain id columns, so no join is needed
     * (the old query joined the accounts table to compare account numbers). Sorting comes from the Pageable.
     */
    @Query("""
            SELECT t FROM TransactionJpaEntity t
            WHERE t.fromAccountId = :accountId
               OR t.toAccountId = :accountId
            """)
    Page<TransactionJpaEntity> findAllInvolvingAccount(@Param("accountId") Long accountId, Pageable pageable);

    /** True if the account sent or received any transaction (cash deposits included). */
    boolean existsByFromAccountIdOrToAccountId(Long fromAccountId, Long toAccountId);
}
