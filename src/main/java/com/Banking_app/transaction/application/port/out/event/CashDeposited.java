package com.Banking_app.transaction.application.port.out.event;

import com.Banking_app.transaction.domain.models.transaction.Transaction;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** A Cash Deposit Machine top-up was credited to an account. */
public record CashDeposited(UUID eventId,
                            Instant occurredAt,
                            String reference,
                            String accountNumber,
                            String ownerId,
                            BigDecimal amount) implements TransactionEvent {

    public static final String TYPE = "CASH_DEPOSITED";
    public static final int VERSION = 1;

    /** The event of a top-up that has just been saved. */
    public static CashDeposited of(Transaction saved, String accountNumber, String ownerId) {
        return new CashDeposited(UUID.randomUUID(), saved.getSuccessAt(), saved.getReferenceNumber(),
                accountNumber, ownerId, saved.getAmount());
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
        return accountNumber;
    }
}
