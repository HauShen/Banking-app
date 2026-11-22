package com.Banking_app.transaction.application.port.out.event;

import com.Banking_app.transaction.application.port.out.TransferParty;
import com.Banking_app.transaction.domain.models.transaction.Transaction;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Money moved from one account to another. Filed under the from-account: all money leaving one account is read in
 * order (what a "many transfers in a few minutes" alert needs).
 *
 * @param amount   the MYR amount that moved
 * @param original the amount as the customer stated it in a foreign currency, or null for a MYR transfer
 */
public record TransferCompleted(UUID eventId,
                                Instant occurredAt,
                                String reference,
                                String fromAccountNumber,
                                String fromOwnerId,
                                String toAccountNumber,
                                String toOwnerId,
                                BigDecimal amount,
                                ForeignAmount original) implements TransactionEvent {

    public static final String TYPE = "TRANSFER_COMPLETED";
    public static final int VERSION = 1;

    /** The event of a transfer that has just been saved. */
    public static TransferCompleted of(Transaction saved, TransferParty from, TransferParty to) {
        return new TransferCompleted(UUID.randomUUID(), saved.getSuccessAt(), saved.getReferenceNumber(),
                from.accountNumber(), from.ownerId(), to.accountNumber(), to.ownerId(),
                saved.getAmount(), ForeignAmount.of(saved.getConversion()));
    }

    @Override
    public String eventType() {
        return TYPE;
    }

    @Override
    public int version() {
        return VERSION;
    }

    @Override
    public String orderingKey() {
        return fromAccountNumber;
    }
}
