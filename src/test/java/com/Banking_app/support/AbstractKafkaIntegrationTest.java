package com.Banking_app.support;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.kafka.KafkaContainer;

/**
 * Base class for tests that need a real Kafka broker, on top of everything {@link AbstractIntegrationTest} gives
 * (PostgreSQL, the HTTP helpers).
 *
 * <ul>
 *   <li>One Kafka container for the whole run, like PostgreSQL. The GraalVM "native" image of the same Kafka version
 *       as Docker Compose starts in about a second (fine for tests; Compose uses the normal JVM image).</li>
 *   <li>Turns {@code app.kafka.enabled} on. These tests get their own Spring context (different properties), the
 *       rest of the suite keeps running without a broker.</li>
 *   <li>Tests never assume an empty topic: each test looks only for its own messages (its own key or event id).</li>
 * </ul>
 */
public abstract class AbstractKafkaIntegrationTest extends AbstractIntegrationTest {

    static final KafkaContainer KAFKA = new KafkaContainer("apache/kafka-native:4.3.1");

    static {
        KAFKA.start();   // stopped automatically by Testcontainers when the JVM exits
    }

    @DynamicPropertySource
    static void kafka(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
        registry.add("app.kafka.enabled", () -> "true");
    }
}
