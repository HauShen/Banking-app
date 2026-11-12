package com.Banking_app.security;

import com.Banking_app.support.AbstractIntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Are you up? The health endpoint is the one URL outside /api/auth/** that works without a token:
 * Docker, the CI smoke test and a hosting platform must be able to ask it without logging in.
 * It may say UP or DOWN and nothing else, and it must not open the rest of Actuator.
 */
class HealthEndpointSecurityTest extends AbstractIntegrationTest {

    private static final String HEALTH = "/actuator/health";

    @Test
    @DisplayName("GET /actuator/health without a token -> 200 and status UP")
    void health_isPublic() {
        ResponseEntity<String> response = get(HEALTH, null);

        assertStatus(response, 200);
        assertThat(text(response, "status")).isEqualTo("UP");
    }

    @Test
    @DisplayName("The answer has no details: nothing about the database or the disk")
    void health_showsNoDetails() {
        JsonNode body = json(get(HEALTH, null));

        assertThat(body.has("components")).isFalse();
        assertThat(body.has("details")).isFalse();
    }

    @Test
    @DisplayName("Health sub-paths and the rest of Actuator without a token -> 401")
    void restOfActuator_needsLogin() {
        assertStatus(get(HEALTH + "/db", null), 401);
        assertStatus(get("/actuator/env", null), 401);
        assertStatus(get("/actuator", null), 401);
    }
}
