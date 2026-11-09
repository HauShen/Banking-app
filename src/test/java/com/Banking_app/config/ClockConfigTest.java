package com.Banking_app.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class ClockConfigTest {

    private final Clock clock = new ClockConfig().clock();

    @Test
    @DisplayName("UTC, whole microseconds only (PostgreSQL's timestamp precision)")
    void ticksInMicroseconds() {
        for (int i = 0; i < 100; i++) {
            Instant now = clock.instant();
            assertThat(now.getNano() % 1_000).isZero();
        }
        assertThat(clock.getZone()).isEqualTo(ZoneOffset.UTC);
    }
}
