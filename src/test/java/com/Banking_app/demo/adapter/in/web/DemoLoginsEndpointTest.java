package com.Banking_app.demo.adapter.in.web;

import com.Banking_app.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Without the demo profile (the shared test context) the demo logins don't exist: a plain 404, so the login page
 * shows no demo box. Not 401 (the endpoint is public) and not 500 (an unknown URL would end in the catch-all
 * exception handler). With the profile: DemoProfileStartupTest.
 */
class DemoLoginsEndpointTest extends AbstractIntegrationTest {

    @Test
    void withoutDemoProfile_404WithoutABody() {
        ResponseEntity<String> response = get("/api/demo/logins", null);

        assertThat(response.getStatusCode().value()).isEqualTo(404);
        assertThat(response.getBody()).isNullOrEmpty();
    }
}
