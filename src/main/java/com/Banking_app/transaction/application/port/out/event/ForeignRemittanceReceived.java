package com.Banking_app.transaction.application.port.out.event;

import com.Banking_app.transaction.application.port.out.ReceivingAccount;
import com.Banking_app.transaction.domain.models.transaction.Transaction;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Money from abroad was converted and credited to an account. The sender's name is left out on purpose (it is in the
 * transaction's description, which only the owner and admins can read).
 *
 * @param amount   the MYR amount credited
 * @param original the foreign amount that was received and the buy rate it was converted at
 */
public record ForeignRemittanceReceived(UUID eventId,
                                        Instant occurredAt,
                                        String reference,
                                        String accountNumber,
                                        String ownerId,
                                        BigDecimal amount,
                                        ForeignAmount original) implements TransactionEvent {

    public static final String TYPE = "FOREIGN_REMITTANCE_RECEIVED";
    public static final int VERSION = 1;

    /** The event of a remittance that has just been saved. */
    public static ForeignRemittanceReceived of(Transaction saved, ReceivingAccount account) {
        return new ForeignRemittanceReceived(UUID.randomUUID(), saved.getSuccessAt(), saved.getReferenceNumber(),
                account.accountNumber(), account.ownerId(), saved.getAmount(),
                ForeignAmount.of(saved.getConversion()));
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
