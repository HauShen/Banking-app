package com.Banking_app.notification.config.kafka;

import org.apache.kafka.common.TopicPartition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

/**
 * What happens when the @KafkaListener throws (the same rules as in the main app):
 *
 * <ul>
 *   <li>Any other exception (e.g. the database is briefly unavailable): retried twice, one second apart, on the same
 *       record — 3 tries in all. The partition waits meanwhile, so the order stays the same.</li>
 *   <li>Still failing, or {@link UnprocessableMessageException} (retrying can't help): the record is copied to
 *       {@code <topic>.DLT}, same partition number, with headers saying why, and the consumer moves on.</li>
 * </ul>
 * The dead-letter topic is created by the main app together with the topic itself.
 */
@Configuration
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true")
public class KafkaConsumerConfig {

    static final long RETRY_INTERVAL_MS = 1_000L;
    static final long RETRIES = 2L;

    @Bean
    public DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<String, String> kafkaTemplate) {
        // Named on purpose: the recoverer's default would be <topic>-dlt, a topic that doesn't exist.
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate,
                (record, exception) -> new TopicPartition(record.topic() + KafkaTopics.DLT_SUFFIX, record.partition()));
        DefaultErrorHandler handler = new DefaultErrorHandler(recoverer, new FixedBackOff(RETRY_INTERVAL_MS, RETRIES));
        handler.addNotRetryableExceptions(UnprocessableMessageException.class);
        return handler;
    }
}
