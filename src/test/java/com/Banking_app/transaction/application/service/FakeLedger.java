package com.Banking_app.transaction.application.service;

import com.Banking_app.transaction.application.port.out.LoadLedgerPort;
import com.Banking_app.transaction.domain.enums.LedgerType;
import com.Banking_app.transaction.domain.models.transaction.LedgerEntry;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** Ledger entries in memory, with the same ordering as LedgerPersistenceAdapter (time, then insertion). */
class FakeLedger implements LoadLedgerPort {

    private final List<LedgerEntry> entries = new ArrayList<>();
    Instant lastSince;

    void credit(Long accountId, String amount, String balanceAfter, Instant postedAt) {
        entries.add(new LedgerEntry((long) entries.size() + 1, accountId, LedgerType.CREDIT, new BigDecimal(amount),
                new BigDecimal(balanceAfter), postedAt));
    }

    void debit(Long accountId, String amount, String balanceAfter, Instant postedAt) {
        entries.add(new LedgerEntry((long) entries.size() + 1, accountId, LedgerType.DEBIT, new BigDecimal(amount),
                new BigDecimal(balanceAfter), postedAt));
    }

    private List<LedgerEntry> of(Long accountId) {
        return entries.stream().filter(e -> e.accountId().equals(accountId))
                .sorted(Comparator.comparing(LedgerEntry::postedAt).thenComparing(LedgerEntry::id))
                .toList();
    }

    @Override
    public Optional<LedgerEntry> findLastPostedBefore(Long accountId, Instant before) {
        return of(accountId).stream().filter(e -> e.postedAt().isBefore(before)).reduce((first, second) -> second);
    }

    @Override
    public List<LedgerEntry> findPostedSince(Long accountId, Instant since) {
        lastSince = since;
        return of(accountId).stream().filter(e -> !e.postedAt().isBefore(since)).toList();
    }
}
