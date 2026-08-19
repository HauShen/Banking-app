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

    @Query("""
        SELECT t FROM Transaction t
        LEFT JOIN t.fromAccount fa
        JOIN t.toAccount ta
        WHERE fa.accountNumber = :accountNumber
           OR ta.accountNumber = :accountNumber
        ORDER BY t.createdAt DESC
        """)    Page<Transaction> findAllByAccountNumber(@Param("accountNumber") String accountNumber, Pageable pageable);
}
