package com.Banking_app.security;

import com.Banking_app.support.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Brute-force protection per username. Test settings: 5 failures within 60s lock a username for 2s
 * (src/test/resources/application.properties). Every test uses its own new usernames.
 */
class LoginThrottlingSecurityTest extends AbstractIntegrationTest {

    @Autowired
    private LoginThrottleProperties throttle;

    private ResponseEntity<String> attemptLogin(String username, String password) {
        return post("/api/auth/login", null, Map.of("username", username, "password", password));
    }

    private void failLogins(String username, int times) {
        for (int i = 0; i < times; i++) {
            assertStatus(attemptLogin(username, "wrong-password"), 401);
        }
    }

    private void waitForLockoutToExpire() throws InterruptedException {
        Thread.sleep(throttle.lockout().toMillis() + 500);
    }

    @Test
    @DisplayName("After the maximum failures even the correct password gets 429 with Retry-After")
    void maxFailures_lockTheUsername() {
        String username = uniqueName("brute");
        register(username);

        failLogins(username, throttle.maxFailuresPerUser());
        ResponseEntity<String> response = attemptLogin(username, PASSWORD);

        assertStatus(response, 429);
        assertThat(text(response, "message")).isEqualTo("Too many failed login attempts. Try again later.");
        assertThat(response.getHeaders().getFirst(HttpHeaders.RETRY_AFTER)).isNotBlank();
        assertThat(Long.parseLong(response.getHeaders().getFirst(HttpHeaders.RETRY_AFTER))).isPositive();
    }

    @Test
    @DisplayName("One locked username does not affect other users")
    void lockout_isPerUsername() {
        String victim = uniqueName("locked");
        String bystander = uniqueName("bystander");
        register(victim);
        register(bystander);

        failLogins(victim, throttle.maxFailuresPerUser());

        assertStatus(attemptLogin(victim, PASSWORD), 429);
        assertStatus(attemptLogin(bystander, PASSWORD), 200);
    }

    @Test
    @DisplayName("Unknown usernames are locked the same way, so a lockout doesn't reveal which usernames exist")
    void unknownUsernames_areLockedToo() {
        String ghost = uniqueName("ghost");   // never registered

        failLogins(ghost, throttle.maxFailuresPerUser());

        assertStatus(attemptLogin(ghost, PASSWORD), 429);
    }

    @Test
    @DisplayName("A successful login resets the username's failure count")
    void success_resetsTheCount() {
        String username = uniqueName("reset");
        register(username);
        int belowLimit = throttle.maxFailuresPerUser() - 1;

        failLogins(username, belowLimit);
        assertStatus(attemptLogin(username, PASSWORD), 200);
        failLogins(username, belowLimit);

        assertStatus(attemptLogin(username, PASSWORD), 200);
    }

    @Test
    @DisplayName("The lock expires after the lockout period")
    void lockout_expires() throws InterruptedException {
        String username = uniqueName("expire");
        register(username);
        failLogins(username, throttle.maxFailuresPerUser());
        assertStatus(attemptLogin(username, PASSWORD), 429);

        waitForLockoutToExpire();

        assertStatus(attemptLogin(username, PASSWORD), 200);
    }
}
