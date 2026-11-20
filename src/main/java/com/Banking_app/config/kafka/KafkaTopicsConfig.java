package com.Banking_app.config.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * The topics the app needs. At startup Spring Kafka's KafkaAdmin creates every {@link NewTopic} bean that does not
 * exist on the broker yet (and leaves existing ones alone). The broker's own auto-create is off in Docker Compose,
 * so a misspelled topic name fails instead of quietly creating a new topic.
 *
 * <p>Only when {@code app.kafka.enabled=true}. Without these beans KafkaAdmin never connects, so the normal test
 * suite and a run with {@code KAFKA_ENABLED=false} need no broker.
 */
@Configuration
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true")
public class KafkaTopicsConfig {

    private final int partitions;
    private final int replicas;

    public KafkaTopicsConfig(@Value("${app.kafka.topic-partitions:3}") int partitions,
                             @Value("${app.kafka.topic-replicas:1}") int replicas) {
        this.partitions = partitions;
        this.replicas = replicas;
    }

    /**
     * Ordering is guaranteed only inside one partition. Every event carries the account number as its key, so all
     * events of one account land on the same partition and are read in the order they happened.
     */
    @Bean
    public NewTopic transactionsTopic() {
        return TopicBuilder.name(KafkaTopics.TRANSACTIONS).partitions(partitions).replicas(replicas).build();
    }

    /**
     * The dead-letter publisher writes a failed record to the same partition number it came from, so the DLT needs
     * at least as many partitions as the main topic.
     */
    @Bean
    public NewTopic transactionsDeadLetterTopic() {
        return TopicBuilder.name(KafkaTopics.TRANSACTIONS_DLT).partitions(partitions).replicas(replicas).build();
    }
}
