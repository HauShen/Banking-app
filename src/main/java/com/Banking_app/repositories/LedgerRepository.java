package com.Banking_app.repositories;

import com.Banking_app.models.LedgerEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LedgerRepository extends JpaRepository<LedgerEntry, Long> {
    List<LedgerEntry> findByTransactionReferenceNumber(String TransactionReferenceNumber);
}
