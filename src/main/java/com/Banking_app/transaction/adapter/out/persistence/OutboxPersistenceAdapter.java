package com.Banking_app.transaction.adapter.out.persistence;

import com.Banking_app.config.kafka.KafkaTopics;
import com.Banking_app.transaction.adapter.out.persistence.entities.OutboxEventJpaEntity;
import com.Banking_app.transaction.adapter.out.persistence.jparepositories.OutboxEventJpaRepository;
import com.Banking_app.transaction.application.port.out.PublishTransactionEventPort;
import com.Banking_app.transaction.application.port.out.event.TransactionEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Transactional outbox: "publishing" an event means inserting it into {@code outbox_events}, in the caller's
 * database transaction. Nothing talks to Kafka here; a relay sends the stored rows later.
 *
 * <p>Why not send to Kafka right away? Inside the transaction, the send could succeed and the transaction still roll
 * back (an event about money that never moved); after the commit, the app could crash before sending (a lost event).
 * One insert in the same transaction has neither problem.
 */
@Component
public class OutboxPersistenceAdapter implements PublishTransactionEventPort {

    private final OutboxEventJpaRepository repository;
    private final ObjectMapper objectMapper;

    public OutboxPersistenceAdapter(OutboxEventJpaRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    /**
     * MANDATORY: refuses to run without the caller's transaction. On its own the insert would commit at once, even if
     * the money movement it describes rolls back afterwards.
     */
    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void publish(TransactionEvent event) {
        repository.save(new OutboxEventJpaEntity(event.eventId(), KafkaTopics.TRANSACTIONS, event.orderingKey(),
                event.eventType(), event.version(), toJson(event), event.occurredAt()));
    }

    /** The record's components as JSON, with Spring Boot's ObjectMapper: times as ISO-8601 text, amounts exact. */
    private String toJson(TransactionEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not write " + event.eventType() + " as JSON", e);
        }
    }
}
