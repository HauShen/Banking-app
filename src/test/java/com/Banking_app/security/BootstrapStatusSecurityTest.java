package com.Banking_app.security;

import com.Banking_app.support.AbstractIntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** GET /api/auth/bootstrap-status is public, so it must say no more than a first-run setup needs. */
class BootstrapStatusSecurityTest extends AbstractIntegrationTest {

    @Test
    @DisplayName("public: bootstrapEnabled, adminExists and allowed only — no user count")
    void revealsOnlyTheBootstrapState() {
        register(uniqueName("bs"));   // there are users; the answer must not say how many

        ResponseEntity<String> response = get("/api/auth/bootstrap-status", null);

        assertStatus(response, 200);
        JsonNode body = json(response);
        List<String> fields = new ArrayList<>();
        body.fieldNames().forEachRemaining(fields::add);
        assertThat(fields).containsExactlyInAnyOrder("bootstrapEnabled", "adminExists", "allowed");
        assertThat(body.get("bootstrapEnabled").asBoolean()).isTrue();
        assertThat(body.get("adminExists").asBoolean()).isTrue();    // testadmin from the test properties
        assertThat(body.get("allowed").asBoolean()).isFalse();
    }
}
