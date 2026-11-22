package com.Banking_app.transaction.application.port.out.event;

import java.time.Instant;
import java.util.UUID;

/**
 * A money movement that has happened, told to the rest of the system (notification-service, alerts) through the outbox.
 *
 * <p>Events are facts in the past tense: they are created only after the transaction was saved, and only for a new
 * transaction, never for an idempotent replay. They carry ids, account numbers and amounts, but no names, emails or
 * free text, so a message on the broker reveals as little as possible.
 *
 * <p>The record components are the JSON payload. {@link #eventType()}, {@link #version()} and {@link #orderingKey()}
 * are not components (so not in the payload): the outbox stores them in columns of their own.
 */
public sealed interface TransactionEvent permits TransferCompleted, CashDeposited, ForeignRemittanceReceived {

    /** Unique per event. Consumers use it to recognise a message they have already processed. */
    UUID eventId();

    /** When the money moved (the transaction's completion time). */
    Instant occurredAt();

    /** The transaction's reference number, e.g. TXN-…, CDM-…, RMT-…. */
    String reference();

    /** Stable name of the event, e.g. TRANSFER_COMPLETED: consumers decide by it how to read the payload. */
    String eventType();

    /** Version of the payload format. A change old consumers could not read means a new version. */
    int version();

    /**
     * The account number the event is filed under: the Kafka message key. All events with the same key go to the same
     * partition, so one account's events are read in the order they happened.
     */
    String orderingKey();
}
