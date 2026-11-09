package com.Banking_app.transaction.application.service;

import com.Banking_app.shared.PageResult;
import com.Banking_app.transaction.application.port.out.DuplicateIdempotencyKeyException;
import com.Banking_app.transaction.application.port.out.LoadTransactionPort;
import com.Banking_app.transaction.application.port.out.SaveTransactionPort;
import com.Banking_app.transaction.application.port.out.TransactionSummary;
import com.Banking_app.transaction.domain.enums.TransactionStatus;
import com.Banking_app.transaction.domain.enums.TransactionType;
import com.Banking_app.transaction.domain.models.transaction.Transaction;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** In-memory transactions table for use-case tests. */
class FakeTransactionStore implements SaveTransactionPort, LoadTransactionPort {

    private final List<TransactionSummary> rows = new ArrayList<>();
    private final Map<String, String> keyToReference = new HashMap<>();
    final List<Transaction> saved = new ArrayList<>();
    private long nextId = 100;

    /**
     * Simulates losing a race: the next save fails as if another account had stored the same idempotency key
     * after our lookup (which therefore found nothing).
     */
    boolean nextSaveLosesTheRace;

    /** Puts an existing transaction in the table (e.g. an earlier transfer with the same idempotency key). */
    TransactionSummary add(String reference, Long fromAccountId, Long toAccountId, String amount,
                           String idempotencyKey, Instant createdAt) {
        TransactionType type = fromAccountId == null ? TransactionType.CASH_DEPOSIT : TransactionType.TRANSFER;
        TransactionSummary row = new TransactionSummary(nextId++, reference, type, fromAccountId, toAccountId,
                new BigDecimal(amount), "earlier", TransactionStatus.SUCCESS, createdAt, createdAt.plusSeconds(1));
        rows.add(row);
        keyToReference.put(idempotencyKey, reference);
        return row;
    }

    @Override
    public Transaction save(Transaction t) {
        if (nextSaveLosesTheRace || keyToReference.containsKey(t.getIdempotencyKey())) {   // unique constraint
            nextSaveLosesTheRace = false;
            throw new DuplicateIdempotencyKeyException(t.getIdempotencyKey(), null);
        }
        Transaction withId = Transaction.rehydrate(nextId++, t.getReferenceNumber(), t.getType(), t.getFromAccountId(),
                t.getToAccountId(), t.getAmount(), t.getDescription(), t.getStatus(), t.getCreatedAt(),
                t.getSuccessAt(), t.getIdempotencyKey(), t.getLedgerEntries());
        saved.add(withId);
        rows.add(new TransactionSummary(withId.getId(), withId.getReferenceNumber(), withId.getType(),
                withId.getFromAccountId(),
                withId.getToAccountId(), withId.getAmount(), withId.getDescription(), withId.getStatus(),
                withId.getCreatedAt(), withId.getSuccessAt()));
        keyToReference.put(withId.getIdempotencyKey(), withId.getReferenceNumber());
        return withId;
    }

    @Override
    public Optional<TransactionSummary> findByReferenceNumber(String referenceNumber) {
        return rows.stream().filter(r -> r.referenceNumber().equals(referenceNumber)).findFirst();
    }

    @Override
    public Optional<TransactionSummary> findByIdempotencyKey(String idempotencyKey) {
        return Optional.ofNullable(keyToReference.get(idempotencyKey)).flatMap(this::findByReferenceNumber);
    }

    @Override
    public PageResult<TransactionSummary> findPageInvolvingAccount(Long accountId, int page, int size) {
        List<TransactionSummary> all = rows.stream()
                .filter(r -> r.isSentFrom(accountId) || r.toAccountId().equals(accountId))
                .sorted(Comparator.comparing(TransactionSummary::createdAt).reversed())
                .toList();
        List<TransactionSummary> slice = all.stream().skip((long) page * size).limit(size).toList();
        return new PageResult<>(slice, page, size, all.size());
    }

    @Override
    public boolean existsInvolvingAccount(Long accountId) {
        return rows.stream().anyMatch(r -> r.isSentFrom(accountId) || r.toAccountId().equals(accountId));
    }
}
