package com.Banking_app.repositories;

import com.Banking_app.models.LedgerEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface LedgerRepository extends JpaRepository<LedgerEntry, Long> {
    List<LedgerEntry> findByTransactionReferenceNumber(String TransactionReferenceNumber);
    @Query("""
        SELECT l
        FROM LedgerEntry l
        JOIN FETCH l.transaction t
        JOIN FETCH l.account a
        WHERE a.accountNumber = :accountNumber
    """)
    Page<LedgerEntry> findStatementByAccountNumber(@Param("accountNumber") String accountNumber, Pageable pageable);
}
