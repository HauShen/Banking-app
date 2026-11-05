package com.Banking_app.transaction.adapter.out.persistence.mappers;

import com.Banking_app.transaction.adapter.out.persistence.entities.LedgerEntryJpaEntity;
import com.Banking_app.transaction.adapter.out.persistence.entities.TransactionJpaEntity;
import com.Banking_app.transaction.application.port.out.TransactionSummary;
import com.Banking_app.transaction.domain.models.transaction.LedgerEntry;
import com.Banking_app.transaction.domain.models.transaction.Transaction;
import org.springframework.stereotype.Component;

/** Domain ↔ JPA entity. No rules here: just copying fields. */
@Component
public class TransactionPersistenceMapper {

    public TransactionJpaEntity toJpaEntity(Transaction transaction) {
        TransactionJpaEntity entity = new TransactionJpaEntity();
        entity.setId(transaction.getId());
        entity.setReferenceNumber(transaction.getReferenceNumber());
        entity.setFromAccountId(transaction.getFromAccountId());
        entity.setToAccountId(transaction.getToAccountId());
        entity.setAmount(transaction.getAmount());
        entity.setStatus(transaction.getStatus());
        entity.setDescription(transaction.getDescription());
        entity.setCreatedAt(transaction.getCreatedAt());
        entity.setSuccessAt(transaction.getSuccessAt());
        entity.setIdempotencyKey(transaction.getIdempotencyKey());
        for (LedgerEntry entry : transaction.getLedgerEntries()) {
            entity.addLedgerEntry(toJpaEntity(entry));
        }
        return entity;
    }

    private LedgerEntryJpaEntity toJpaEntity(LedgerEntry entry) {
        LedgerEntryJpaEntity entity = new LedgerEntryJpaEntity();
        entity.setId(entry.id());
        entity.setAccountId(entry.accountId());
        entity.setLedgerType(entry.type());
        entity.setAmount(entry.amount());
        entity.setBalanceAfter(entry.balanceAfter());
        entity.setPostedAt(entry.postedAt());
        return entity;
    }

    /**
     * Full aggregate, ledger entries included. Only used right after save(), when the entries are the
     * in-memory list we just wrote, so no lazy loading happens.
     */
    public Transaction toDomain(TransactionJpaEntity entity) {
        return Transaction.rehydrate(entity.getId(), entity.getReferenceNumber(), entity.getFromAccountId(),
                entity.getToAccountId(), entity.getAmount(), entity.getDescription(), entity.getStatus(),
                entity.getCreatedAt(), entity.getSuccessAt(), entity.getIdempotencyKey(),
                entity.getLedgerEntries().stream().map(TransactionPersistenceMapper::toDomain).toList());
    }

    private static LedgerEntry toDomain(LedgerEntryJpaEntity entity) {
        return new LedgerEntry(entity.getId(), entity.getAccountId(), entity.getLedgerType(), entity.getAmount(),
                entity.getBalanceAfter(), entity.getPostedAt());
    }

    /** For reads: never touches ledgerEntries, so the LAZY collection is not loaded. */
    public TransactionSummary toSummary(TransactionJpaEntity entity) {
        return new TransactionSummary(entity.getId(), entity.getReferenceNumber(), entity.getFromAccountId(),
                entity.getToAccountId(), entity.getAmount(), entity.getDescription(), entity.getStatus(),
                entity.getCreatedAt(), entity.getSuccessAt());
    }
}
