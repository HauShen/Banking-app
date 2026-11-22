package com.Banking_app.alert.application.port.out;

import java.time.Instant;
import java.util.UUID;

/**
 * The alert module's own short memory of outgoing transfers, for the rapid-transfers rule. It may not ask the
 * transaction module, so it keeps what it saw in the messages — only as far back as the rule's window.
 */
public interface TransferLogPort {

    void record(UUID eventId, String accountNumber, Instant occurredAt);

    /** Transfers out of the account with {@code after < occurredAt <= upTo}. */
    long countInWindow(String accountNumber, Instant after, Instant upTo);

    /** Forgets the account's transfers at or before {@code cutoff}: older than any window still needs. */
    void forgetUpTo(String accountNumber, Instant cutoff);
}
