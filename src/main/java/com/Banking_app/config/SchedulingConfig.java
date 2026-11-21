package com.Banking_app.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Turns on @Scheduled methods. Today these are the outbox relay and its housekeeping, which only exist with
 * {@code app.kafka.enabled=true}; without them the scheduler has nothing to run.
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
