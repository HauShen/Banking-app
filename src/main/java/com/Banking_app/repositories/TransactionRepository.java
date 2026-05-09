package com.Banking_app.repositories;

import com.Banking_app.models.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    Optional<Transaction> findByReferenceNumber(String reference);
    Optional<Transaction> findByIdempotencyKey(String idempotencyKey);
    List<Transaction> findAllByAccountId(Long accountId);
    boolean existsByIdempotencyKey(String idempotencyKey);
}
