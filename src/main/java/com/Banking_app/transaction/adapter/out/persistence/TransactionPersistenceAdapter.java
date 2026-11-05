package com.Banking_app.transaction.adapter.out.persistence;

import com.Banking_app.shared.PageResult;
import com.Banking_app.transaction.adapter.out.persistence.jparepositories.TransactionJpaRepository;
import com.Banking_app.transaction.adapter.out.persistence.mappers.TransactionPersistenceMapper;
import com.Banking_app.transaction.application.port.out.LoadTransactionPort;
import com.Banking_app.transaction.application.port.out.SaveTransactionPort;
import com.Banking_app.transaction.application.port.out.TransactionSummary;
import com.Banking_app.transaction.domain.models.transaction.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class TransactionPersistenceAdapter implements SaveTransactionPort, LoadTransactionPort {

    /** Newest first, as before; id as tie-breaker so paging is stable when two rows share a timestamp. */
    private static final Sort NEWEST_FIRST = Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));

    private final TransactionJpaRepository transactionJpaRepository;
    private final TransactionPersistenceMapper mapper;

    public TransactionPersistenceAdapter(TransactionJpaRepository transactionJpaRepository,
                                         TransactionPersistenceMapper mapper) {
        this.transactionJpaRepository = transactionJpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Transaction save(Transaction transaction) {
        return mapper.toDomain(transactionJpaRepository.save(mapper.toJpaEntity(transaction)));
    }

    @Override
    public Optional<TransactionSummary> findByReferenceNumber(String referenceNumber) {
        return transactionJpaRepository.findByReferenceNumber(referenceNumber).map(mapper::toSummary);
    }

    @Override
    public Optional<TransactionSummary> findByIdempotencyKey(String idempotencyKey) {
        return transactionJpaRepository.findByIdempotencyKey(idempotencyKey).map(mapper::toSummary);
    }

    @Override
    public PageResult<TransactionSummary> findPageInvolvingAccount(Long accountId, int page, int size) {
        // PageRequest.of rejects a negative page or size < 1 with IllegalArgumentException → 400, as before.
        Page<TransactionSummary> rows = transactionJpaRepository
                .findAllInvolvingAccount(accountId, PageRequest.of(page, size, NEWEST_FIRST))
                .map(mapper::toSummary);
        return new PageResult<>(rows.getContent(), rows.getNumber(), rows.getSize(), rows.getTotalElements());
    }

    @Override
    public boolean existsInvolvingAccount(Long accountId) {
        return transactionJpaRepository.existsByFromAccountIdOrToAccountId(accountId, accountId);
    }
}
