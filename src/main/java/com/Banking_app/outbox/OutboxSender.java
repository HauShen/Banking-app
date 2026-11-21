package com.Banking_app.outbox;

/** Delivers one outbox message to the broker. */
public interface OutboxSender {

    /** Returns only when the broker has confirmed the message; throws if it did not (in time). */
    void send(OutboxMessage message) throws Exception;
}
