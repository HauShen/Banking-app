package com.Banking_app.notification.support;

import com.Banking_app.notification.config.kafka.KafkaTopics;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.kafka.KafkaContainer;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Base class for tests that need a real Kafka broker, on top of {@link AbstractIntegrationTest}.
 *
 * <ul>
 *   <li>One Kafka container for the whole run (the "native" image of the same version as Docker Compose).</li>
 *   <li>The main app creates the topics in real life. Here the base class creates them, the same way: 3 partitions
 *       each, and the dead-letter topic too (dead-lettering keeps the partition number).</li>
 *   <li>Turns {@code app.kafka.enabled} on, so the listener runs; {@code @DirtiesContext} stops it after the class.</li>
 * </ul>
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public abstract class AbstractKafkaIntegrationTest extends AbstractIntegrationTest {

    static final KafkaContainer KAFKA = new ReliableKafkaContainer("apache/kafka-native:4.3.1");   // see that class: fixes a start-up race

    static {
        KAFKA.start();
        try (Admin admin = Admin.create(Map.of(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers()))) {
            admin.createTopics(List.of(
                    new NewTopic(KafkaTopics.TRANSACTIONS, 3, (short) 1),
                    new NewTopic(KafkaTopics.TRANSACTIONS_DLT, 3, (short) 1))).all().get(30, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new IllegalStateException("Could not create the test topics", e);
        }
    }

    @DynamicPropertySource
    static void kafka(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
        registry.add("app.kafka.enabled", () -> "true");
        // the test application.properties replaces the main one: restate what the listener needs
        // (Spring Boot's default serializers are the String ones already)
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
