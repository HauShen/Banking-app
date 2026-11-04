package com.Banking_app.security;

import com.Banking_app.support.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Browsers ask the API (a CORS "preflight" OPTIONS request) before letting JavaScript from another origin
 * call it. Only the configured frontend origin and the headers it really sends are allowed.
 */
class CorsSecurityTest extends AbstractIntegrationTest {

    /** Default of app.cors.allowed-origins (the test properties don't override it). */
    private static final String FRONTEND = "http://localhost:5173";

    private ResponseEntity<String> preflight(String origin, String requestHeaders) {
        return http.options().uri("/api/accounts/summary")
                .header("Origin", origin)
                .header("Access-Control-Request-Method", "GET")
                .header("Access-Control-Request-Headers", requestHeaders)
                .retrieve().toEntity(String.class);
    }

    @Test
    @DisplayName("Preflight from the configured frontend origin is allowed")
    void preflightFromFrontend_isAllowed() {
        ResponseEntity<String> response = preflight(FRONTEND, "authorization,content-type");

        assertStatus(response, 200);
        assertThat(response.getHeaders().getAccessControlAllowOrigin()).isEqualTo(FRONTEND);
    }

    @Test
    @DisplayName("Preflight from any other site is rejected")
    void preflightFromAnotherSite_isRejected() {
        ResponseEntity<String> response = preflight("https://evil.example", "authorization");

        assertStatus(response, 403);
        assertThat(response.getHeaders().getAccessControlAllowOrigin()).isNull();
    }

    @Test
    @DisplayName("Preflight asking for a header the API does not accept is rejected")
    void preflightWithUnlistedHeader_isRejected() {
        assertStatus(preflight(FRONTEND, "x-custom-header"), 403);
    }
}
