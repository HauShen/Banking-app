package com.Banking_app.config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.Duration;

@Configuration
public class ClockConfig {

    /**
     * UTC, ticking in whole microseconds. PostgreSQL stores timestamps to the microsecond, while Java's clock can be
     * finer (100 ns on Windows). With a finer clock, a value returned straight after saving (e.g. a transfer reply's
     * createdAt) could differ in the 7th decimal from the same value read back later (e.g. a duplicate reply).
     * Ticking in microseconds makes "what we return" and "what we stored" identical.
     */
    @Bean
    public Clock clock() {
        return Clock.tick(Clock.systemUTC(), Duration.ofNanos(1_000));
    }
}
