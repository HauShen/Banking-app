package com.Banking_app.repositories;

import com.Banking_app.jpaentities.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    Optional<Transaction> findByReferenceNumber(String reference);
    Optional<Transaction> findByIdempotencyKey(String idempotencyKey);
    boolean existsByIdempotencyKey(String idempotencyKey);

    /**
     * True if the account is the sender or receiver of any transaction (top-ups have no sender,
     * hence the LEFT JOIN). Used to refuse deleting an account with history.
     */
    @Query("""
        SELECT CASE WHEN COUNT(t) > 0 THEN true ELSE false END
        FROM Transaction t
        LEFT JOIN t.fromAccount fa
        JOIN t.toAccount ta
        WHERE fa.accountId = :accountId
           OR ta.accountId = :accountId
        """)
    boolean existsInvolvingAccount(@Param("accountId") Long accountId);

    @Query("""
        SELECT t FROM Transaction t
        LEFT JOIN t.fromAccount fa
        JOIN t.toAccount ta
        WHERE fa.accountNumber = :accountNumber
           OR ta.accountNumber = :accountNumber
        ORDER BY t.createdAt DESC
        """)    Page<Transaction> findAllByAccountNumber(@Param("accountNumber") String accountNumber, Pageable pageable);
}
