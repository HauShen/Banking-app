package com.Banking_app.transaction.application.port.out;

import com.Banking_app.transaction.domain.models.transaction.LedgerEntry;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/** Read side of the ledger: one account's entries, for its balance history. */
public interface LoadLedgerPort {

    /** The account's last entry posted before {@code before}, if any. */
    Optional<LedgerEntry> findLastPostedBefore(Long accountId, Instant before);

    /** The account's entries posted at or after {@code since}, oldest first (ties in posting order). */
    List<LedgerEntry> findPostedSince(Long accountId, Instant since);
}
