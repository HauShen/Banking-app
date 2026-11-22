package com.Banking_app.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Turns on @Scheduled methods. Today these are the outbox relay, its housekeeping and the Kafka topic check, which
 * only exist with {@code app.kafka.enabled=true}; without them the scheduler has nothing to run. The scheduler's
 * thread count is {@code spring.task.scheduling.pool.size} (application.properties).
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
