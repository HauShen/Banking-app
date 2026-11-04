package com.Banking_app.security;

import com.Banking_app.support.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;

import java.util.Map;

/**
 * Brute-force protection per client IP: one client trying a password against many usernames.
 *
 * All test requests come from 127.0.0.1, so a low IP limit would block logins in every other test.
 * The @TestPropertySource gives this class its own Spring context (and its own LoginAttemptService),
 * so the low limit only exists here. It shares the PostgreSQL container with the other tests.
 */
@TestPropertySource(properties = "security.login.max-failures-per-ip=3")
class LoginIpThrottlingSecurityTest extends AbstractIntegrationTest {

    @Autowired
    private LoginThrottleProperties throttle;

    private ResponseEntity<String> attemptLogin(String username, String password) {
        return post("/api/auth/login", null, Map.of("username", username, "password", password));
    }

    @Test
    @DisplayName("Failures spread over many usernames block the IP, then the block expires")
    void failuresFromOneIp_blockThatIp() throws InterruptedException {
        String victim = uniqueName("victim");
        register(victim);

        // Each username fails only once, so no username is locked - only the IP counter grows.
        for (int i = 0; i < throttle.maxFailuresPerIp(); i++) {
            assertStatus(attemptLogin(uniqueName("spray"), "wrong-password"), 401);
        }
        try {
            assertStatus(attemptLogin(victim, PASSWORD), 429);   // correct password, but the IP is blocked
        } finally {
            Thread.sleep(throttle.lockout().toMillis() + 500);
        }

        assertStatus(attemptLogin(victim, PASSWORD), 200);
    }
}
