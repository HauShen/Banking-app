package com.Banking_app.transaction.application.port.out;

import com.Banking_app.transaction.application.port.out.event.TransactionEvent;

/**
 * Hands an event over for delivery to the rest of the system.
 *
 * <p>Must be called inside the database transaction that moved the money. The adapter stores the event in the
 * outbox table (transactional outbox): it is committed together with the money, or rolled back with it. A separate
 * relay sends stored events to Kafka afterwards, so a transfer never waits for Kafka and never fails because of it.
 */
public interface PublishTransactionEventPort {

    void publish(TransactionEvent event);
}
