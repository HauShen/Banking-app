package com.Banking_app.transaction.application.service;

import com.Banking_app.transaction.application.port.out.PublishTransactionEventPort;
import com.Banking_app.transaction.application.port.out.event.TransactionEvent;

import java.util.ArrayList;
import java.util.List;

/** Stands in for the outbox: keeps every published event in a list, in order. */
class FakeTransactionEvents implements PublishTransactionEventPort {

    final List<TransactionEvent> published = new ArrayList<>();

    @Override
    public void publish(TransactionEvent event) {
        published.add(event);
    }
}
