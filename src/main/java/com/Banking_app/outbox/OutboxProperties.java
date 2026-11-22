package com.Banking_app.outbox;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

/**
 * Settings of the outbox relay ({@code app.outbox.*}). How often the scheduled jobs run is set in
 * application.properties next to these (relay-interval, backlog-check-interval, cleanup-interval).
 *
 * @param batchSize           events read and sent per database transaction
 * @param sendTimeout         how long one send may wait for the broker's confirmation
 * @param backlogWarningAfter warn when the oldest waiting event is older than this (e.g. Kafka has been down)
 * @param retention           published events are deleted after this long; unpublished ones are kept
 */
@ConfigurationProperties(prefix = "app.outbox")
public record OutboxProperties(
        @DefaultValue("100") int batchSize,
        @DefaultValue("15s") Duration sendTimeout,
        @DefaultValue("1m") Duration backlogWarningAfter,
        @DefaultValue("7d") Duration retention) {
}
