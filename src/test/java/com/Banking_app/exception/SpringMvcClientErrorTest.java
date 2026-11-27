package com.Banking_app.exception;

import com.Banking_app.support.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Spring MVC's own client errors get their real 4xx status and the usual ApiError body, never the 500 of the
 * catch-all handler. Logged in as admin, so the requests get past security and reach Spring MVC.
 */
class SpringMvcClientErrorTest extends AbstractIntegrationTest {

    @Test
    @DisplayName("URL without an endpoint → 404")
    void unknownUrl() {
        ResponseEntity<String> response = get("/api/no-such-endpoint", adminToken());

        assertStatus(response, 404);
        assertThat(text(response, "message")).isEqualTo("No endpoint GET /api/no-such-endpoint");
        assertThat(text(response, "path")).isEqualTo("/api/no-such-endpoint");
    }

    @Test
    @DisplayName("Endpoint that doesn't take the HTTP method → 405 with the Allow header")
    void wrongMethod() {
        ResponseEntity<String> response = delete("/api/users/me", adminToken());   // GET and PUT only

        assertStatus(response, 405);
        assertThat(text(response, "message")).isEqualTo("Method DELETE is not supported for this URL");
        assertThat(response.getHeaders().getAllow()).contains(HttpMethod.GET, HttpMethod.PUT);
    }

    @Test
    @DisplayName("Body that isn't valid JSON → 400")
    void malformedJson() {
        // The helper sends a String as it is, so this is the broken JSON itself.
        ResponseEntity<String> response = post("/api/admin/users/customers", adminToken(), "{\"username\": ");

        assertStatus(response, 400);
        assertThat(text(response, "message")).isEqualTo("Malformed request body");
    }

    @Test
    @DisplayName("Content-Type the endpoint doesn't take → 415")
    void unsupportedContentType() {
        String token = adminToken();

        ResponseEntity<String> response = http.post().uri("/api/admin/users/customers")
                .headers(h -> h.setBearerAuth(token))
                .contentType(MediaType.TEXT_PLAIN)
                .body("username=someone")
                .retrieve().toEntity(String.class);

        assertStatus(response, 415);
        assertThat(text(response, "message")).startsWith("Content type 'text/plain").endsWith("is not supported");
    }
}
