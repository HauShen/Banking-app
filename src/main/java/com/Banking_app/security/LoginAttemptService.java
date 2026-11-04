package com.Banking_app.security;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Brute-force protection for POST /api/auth/login.
 *
 * <ul>
 *   <li><b>Per username:</b> {@code maxFailuresPerUser} failures within {@code window} lock that username for
 *       {@code lockout}. This also applies to usernames that don't exist, so a lockout never reveals which
 *       usernames are real. A successful login clears the username's counter.</li>
 *   <li><b>Per client IP:</b> {@code maxFailuresPerIp} failures within {@code window} block that IP for
 *       {@code lockout}. Stops one client trying a common password against many usernames. A success does
 *       not clear it, so an attacker can't reset it by logging into their own account.</li>
 * </ul>
 *
 * Known limits (documented in the README): state is in memory, so it resets on restart and is not shared
 * between several instances (production answer: Redis, or rate limiting at the API gateway). The check and the
 * recording are not one atomic step, so a burst of parallel requests can exceed a limit by a few attempts.
 */
@Component
public class LoginAttemptService {

    /** Above this many tracked keys, entries with no recent failures and no active lock are removed. */
    private static final int EVICTION_THRESHOLD = 10_000;

    private final LoginThrottleProperties properties;
    private final Clock clock;
    private final Map<String, Attempts> byUsername = new ConcurrentHashMap<>();
    private final Map<String, Attempts> byIp = new ConcurrentHashMap<>();

    public LoginAttemptService(LoginThrottleProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    /** How long this login must still wait, or empty if it may be attempted now. */
    public Optional<Duration> blockedFor(String username, String clientIp) {
        Instant now = clock.instant();
        Duration userWait = remaining(byUsername.get(key(username)), now);
        Duration ipWait = remaining(byIp.get(clientIp), now);
        Duration wait = userWait.compareTo(ipWait) >= 0 ? userWait : ipWait;
        return wait.isZero() ? Optional.empty() : Optional.of(wait);
    }

    public void recordFailure(String username, String clientIp) {
        Instant now = clock.instant();
        register(byUsername, key(username), properties.maxFailuresPerUser(), now);
        register(byIp, clientIp, properties.maxFailuresPerIp(), now);
    }

    public void recordSuccess(String username) {
        byUsername.remove(key(username));
    }

    private void register(Map<String, Attempts> attemptsByKey, String key, int maxFailures, Instant now) {
        if (attemptsByKey.size() > EVICTION_THRESHOLD) {
            // Someone spraying random usernames must not be able to grow this map forever.
            attemptsByKey.entrySet().removeIf(entry -> entry.getValue().isStale(now, properties.window()));
        }
        attemptsByKey.computeIfAbsent(key, k -> new Attempts())
                .recordFailure(now, maxFailures, properties.window(), properties.lockout());
    }

    private static Duration remaining(Attempts attempts, Instant now) {
        return attempts == null ? Duration.ZERO : attempts.remainingLock(now);
    }

    /** Usernames are compared case-insensitively, so "Alice" and "alice" share one counter. */
    private static String key(String username) {
        return username.trim().toLowerCase(Locale.ROOT);
    }

    /** Failures of one username or one IP. */
    private static final class Attempts {
        private final Deque<Instant> failures = new ArrayDeque<>();
        private Instant lockedUntil = Instant.EPOCH;

        synchronized void recordFailure(Instant now, int maxFailures, Duration window, Duration lockout) {
            Instant windowStart = now.minus(window);
            while (!failures.isEmpty() && failures.peekFirst().isBefore(windowStart)) {
                failures.pollFirst();   // older than the window: no longer counts
            }
            failures.addLast(now);
            if (failures.size() >= maxFailures) {
                lockedUntil = now.plus(lockout);
                failures.clear();       // after the lockout, the count starts again from zero
            }
        }

        synchronized Duration remainingLock(Instant now) {
            return now.isBefore(lockedUntil) ? Duration.between(now, lockedUntil) : Duration.ZERO;
        }

        synchronized boolean isStale(Instant now, Duration window) {
            boolean locked = now.isBefore(lockedUntil);
            boolean recentFailure = !failures.isEmpty() && !failures.peekLast().isBefore(now.minus(window));
            return !locked && !recentFailure;
        }
    }
}
