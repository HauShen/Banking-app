package com.Banking_app.config.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.function.BooleanSupplier;

/**
 * Makes sure the topics exist even when the broker came up after the app.
 *
 * <p>Spring Kafka's KafkaAdmin creates the {@code NewTopic}s (KafkaTopicsConfig) once, at startup. If the broker isn't
 * reachable then, it logs "Could not configure topics" and never tries again. With the broker's auto-create off, a
 * brand-new broker (new environment, lost volume) would then have no topics, and the outbox relay would fail with
 * "Topic … not present in metadata" until someone restarts the app. (Found while planning the failure drills,
 * docs/kafka-failure-drills.md, drill 4.)
 *
 * <p>So: every 30 s, until it has worked once, ask KafkaAdmin again. When the broker is up, the call is cheap
 * (existing topics are left alone), so on a normal start it succeeds on the first try and stops.
 */
@Component
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true")
public class KafkaTopicsInitializer {

    private static final Logger log = LoggerFactory.getLogger(KafkaTopicsInitializer.class);

    private final BooleanSupplier createTopics;
    private volatile boolean done;

    @Autowired
    public KafkaTopicsInitializer(KafkaAdmin kafkaAdmin) {
        this(kafkaAdmin::initialize);
    }

    /** For tests: anything that creates the topics and says whether it worked. */
    KafkaTopicsInitializer(BooleanSupplier createTopics) {
        this.createTopics = createTopics;
    }

    @Scheduled(fixedDelayString = "${app.kafka.topic-check-interval:PT30S}",
            initialDelayString = "${app.kafka.topic-check-interval:PT30S}")
    public void ensureTopics() {
        if (done) {
            return;
        }
        try {
            done = createTopics.getAsBoolean();
        } catch (RuntimeException e) {
            // After startup KafkaAdmin throws instead of logging; the cause says why (e.g. broker unreachable).
            log.warn("Kafka topics are not in place yet, will try again: {}",
                    e.getCause() != null ? e.getCause().toString() : e.toString());
            return;
        }
        if (done) {
            log.info("Kafka topics are in place");
        } else {
            log.warn("Kafka topics are not in place yet, will try again");
        }
    }

    boolean isDone() {
        return done;
    }
}
