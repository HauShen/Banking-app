package com.Banking_app.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

/**
 * Limits for failed logins ({@code security.login.*}). Durations accept Spring Boot formats like {@code 15m} or {@code 2s}.
 *
 * @param maxFailuresPerUser failures for one username within {@code window} before it is locked
 * @param maxFailuresPerIp   failures from one client IP within {@code window} before it is blocked
 * @param window             how far back failures are counted
 * @param lockout            how long a locked username / blocked IP has to wait
 */
@ConfigurationProperties(prefix = "security.login")
public record LoginThrottleProperties(
        @DefaultValue("5") int maxFailuresPerUser,
        @DefaultValue("20") int maxFailuresPerIp,
        @DefaultValue("15m") Duration window,
        @DefaultValue("15m") Duration lockout) {
}
