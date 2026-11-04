package com.Banking_app.security.refresh;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

/**
 * {@code security.refresh.*}
 *
 * @param lifetime     how long a refresh token is valid; every refresh issues a new one with a fresh lifetime
 * @param cookieSecure send the cookie only over HTTPS (browsers also accept it on http://localhost)
 */
@ConfigurationProperties(prefix = "security.refresh")
public record RefreshTokenProperties(
        @DefaultValue("7d") Duration lifetime,
        @DefaultValue("true") boolean cookieSecure) {
}
