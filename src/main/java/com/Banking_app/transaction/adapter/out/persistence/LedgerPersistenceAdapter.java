package com.Banking_app.transaction.adapter.out.persistence;

import com.Banking_app.transaction.adapter.out.persistence.jparepositories.LedgerEntryJpaRepository;
import com.Banking_app.transaction.adapter.out.persistence.mappers.TransactionPersistenceMapper;
import com.Banking_app.transaction.application.port.out.LoadLedgerPort;
import com.Banking_app.transaction.domain.models.transaction.LedgerEntry;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * LoadLedgerPort on the ledgers table. Both queries use the (account_id, created_at, id) index (V11); the id breaks
 * ties between entries posted in the same microsecond, so "last" means the last one written.
 */
@Component
public class LedgerPersistenceAdapter implements LoadLedgerPort {

    private final LedgerEntryJpaRepository ledgerEntryJpaRepository;

    public LedgerPersistenceAdapter(LedgerEntryJpaRepository ledgerEntryJpaRepository) {
        this.ledgerEntryJpaRepository = ledgerEntryJpaRepository;
    }

    @Override
    public Optional<LedgerEntry> findLastPostedBefore(Long accountId, Instant before) {
        return ledgerEntryJpaRepository.findFirstByAccountIdAndPostedAtBeforeOrderByPostedAtDescIdDesc(accountId, before)
                .map(TransactionPersistenceMapper::toDomain);
    }

    @Override
    public List<LedgerEntry> findPostedSince(Long accountId, Instant since) {
        return ledgerEntryJpaRepository.findByAccountIdAndPostedAtGreaterThanEqualOrderByPostedAtAscIdAsc(accountId, since)
                .stream().map(TransactionPersistenceMapper::toDomain).toList();
    }
}
