package com.Banking_app.transaction.adapter.out.persistence.jparepositories;

import com.Banking_app.transaction.adapter.out.persistence.entities.LedgerEntryJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/** Reads of the ledgers table; entries are written together with their transaction (TransactionJpaRepository). */
public interface LedgerEntryJpaRepository extends JpaRepository<LedgerEntryJpaEntity, Long> {

    Optional<LedgerEntryJpaEntity> findFirstByAccountIdAndPostedAtBeforeOrderByPostedAtDescIdDesc(
            Long accountId, Instant before);

    List<LedgerEntryJpaEntity> findByAccountIdAndPostedAtGreaterThanEqualOrderByPostedAtAscIdAsc(
            Long accountId, Instant since);
}
