package com.Banking_app.security;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * LoginAttemptService without Spring: a fixed clock that the test moves forward. The integration tests
 * (LoginThrottlingSecurityTest, LoginIpThrottlingSecurityTest) cover the HTTP side; this covers the exemption for
 * the live demo's shared logins (plan step C2), which only the demo profile switches on.
 */
class LoginAttemptServiceTest {

    private static final int PER_USER = 5;
    private static final int PER_IP = 20;
    private static final Duration FIFTEEN_MINUTES = Duration.ofMinutes(15);

    private final MutableClock clock = new MutableClock(Instant.parse("2026-11-28T00:00:00Z"));

    private LoginAttemptService service(Set<String> unthrottled) {
        return new LoginAttemptService(
                new LoginThrottleProperties(PER_USER, PER_IP, FIFTEEN_MINUTES, FIFTEEN_MINUTES, unthrottled), clock);
    }

    @Test
    void aUsername_isLockedAfterTheMaximumFailures() {
        LoginAttemptService attempts = service(Set.of("weiming"));

        failFromDifferentIps(attempts, "alice", PER_USER);

        assertThat(attempts.blockedFor("alice", "10.9.9.9")).hasValue(FIFTEEN_MINUTES);
    }

    @Test
    void anUnthrottledUsername_isNeverLocked() {
        LoginAttemptService attempts = service(Set.of("weiming", "demoadmin"));

        failFromDifferentIps(attempts, "weiming", PER_USER * 10);
        failFromDifferentIps(attempts, "demoadmin", PER_USER * 10);

        assertThat(attempts.blockedFor("weiming", "10.9.9.9")).isEmpty();
        assertThat(attempts.blockedFor("demoadmin", "10.9.9.9")).isEmpty();
    }

    @Test
    void theExemption_ignoresCaseAndSpaces_likeTheCounter() {
        LoginAttemptService attempts = service(Set.of(" WeiMing "));

        failFromDifferentIps(attempts, "WEIMING", PER_USER);

        assertThat(attempts.blockedFor("weiming", "10.9.9.9")).isEmpty();
    }

    @Test
    void anUnthrottledUsername_stillCountsTowardsThePerIpLimit() {
        LoginAttemptService attempts = service(Set.of("weiming"));

        for (int i = 0; i < PER_IP; i++) {
            attempts.recordFailure("weiming", "10.0.0.1");
        }

        assertThat(attempts.blockedFor("weiming", "10.0.0.1")).as("that IP, same user").hasValue(FIFTEEN_MINUTES);
        assertThat(attempts.blockedFor("alice", "10.0.0.1")).as("that IP, any user").hasValue(FIFTEEN_MINUTES);
        assertThat(attempts.blockedFor("weiming", "10.0.0.2")).as("other visitors").isEmpty();
    }

    @Test
    void withoutAnyUnthrottledUsernames_everyoneIsLocked() {
        LoginAttemptService attempts = service(null);   // nothing configured: binds to an empty set

        failFromDifferentIps(attempts, "weiming", PER_USER);

        assertThat(attempts.blockedFor("weiming", "10.9.9.9")).isPresent();
    }

    @Test
    void aLock_endsAfterTheLockout() {
        LoginAttemptService attempts = service(Set.of());
        failFromDifferentIps(attempts, "alice", PER_USER);

        clock.advance(FIFTEEN_MINUTES.plusSeconds(1));

        assertThat(attempts.blockedFor("alice", "10.9.9.9")).isEmpty();
    }

    /** One failure per IP, so only the per-username counter can reach its limit. */
    private static void failFromDifferentIps(LoginAttemptService attempts, String username, int times) {
        for (int i = 0; i < times; i++) {
            attempts.recordFailure(username, "10.0." + (i / 250) + "." + (i % 250 + 1));
        }
    }

    private static final class MutableClock extends Clock {
        private Instant now;

        MutableClock(Instant now) {
            this.now = now;
        }

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public Instant instant() {
            return now;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }
    }
}
