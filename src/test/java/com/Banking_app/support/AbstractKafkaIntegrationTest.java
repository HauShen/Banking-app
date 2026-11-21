package com.Banking_app.support;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.kafka.KafkaContainer;

import java.util.Map;
import java.util.UUID;

/**
 * Base class for tests that need a real Kafka broker, on top of everything {@link AbstractIntegrationTest} gives
 * (PostgreSQL, the HTTP helpers).
 *
 * <ul>
 *   <li>One Kafka container for the whole run, like PostgreSQL. The GraalVM "native" image of the same Kafka version
 *       as Docker Compose starts in about a second (fine for tests; Compose uses the normal JVM image).</li>
 *   <li>Turns {@code app.kafka.enabled} on, so these tests get their own Spring context, with the outbox relay
 *       and the notification consumer running in the background.</li>
 *   <li>{@code @DirtiesContext}: the context (and with it the relay) is closed after each test class. Otherwise the
 *       relay would keep running while the rest of the suite runs, and stamp other tests' outbox rows as published
 *       behind their backs.</li>
 *   <li>The relay sends every waiting row, other test classes' rows included. Tests never assume an empty topic:
 *       each looks only for its own messages (its own key or event id).</li>
 * </ul>
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public abstract class AbstractKafkaIntegrationTest extends AbstractIntegrationTest {

    static final KafkaContainer KAFKA = new KafkaContainer("apache/kafka-native:4.3.1");

    static {
        KAFKA.start();   // stopped automatically by Testcontainers when the JVM exits
    }

    @DynamicPropertySource
    static void kafka(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
        registry.add("app.kafka.enabled", () -> "true");
        // this file replaces the main application.properties: restate what the consumers need
        registry.add("spring.kafka.consumer.auto-offset-reset", () -> "earliest");
    }

    /** A plain consumer of String keys and values in a new consumer group of its own. Close it after use. */
    protected static KafkaConsumer<String, String> newConsumer() {
        return new KafkaConsumer<>(Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers(),
                ConsumerConfig.GROUP_ID_CONFIG, "test-" + UUID.randomUUID(),
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest",
                ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false"),
                new StringDeserializer(), new StringDeserializer());
    }
}
