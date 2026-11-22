package com.Banking_app.notification.support;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.RestClient;
import org.testcontainers.containers.PostgreSQLContainer;

import java.io.UncheckedIOException;
import java.net.http.HttpClient;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Base class for HTTP-level tests of notification-service: the real app on a random port, PostgreSQL 17 in Docker
 * (one container for the whole run), Flyway and ddl-auto=validate as in production.
 *
 * <p>There is no main app here, so the tests make their own access tokens: signed with the test secret and with the
 * same claims as the main app's (sub, uid, role, ver, iat, exp). The main app's AccessTokenClaimsSecurityTest checks
 * that its tokens really have these claims.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class AbstractIntegrationTest {

    protected static final ObjectMapper JSON = new ObjectMapper();

    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17");

    static {
        POSTGRES.start();   // stopped automatically by Testcontainers when the JVM exits
    }

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @LocalServerPort
    private int port;

    @Value("${security.jwt.secret}")
    private String jwtSecret;

    protected RestClient http;

    @BeforeEach
    void createHttpClient() {
        http = RestClient.builder()
                .baseUrl("http://localhost:" + port)
                .requestFactory(new JdkClientHttpRequestFactory(
                        HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build()))
                // Never throw on 4xx/5xx: the tests assert on the status code themselves.
                .defaultStatusHandler(HttpStatusCode::isError, (request, response) -> { })
                .build();
    }

    // ------------------------------------------------------------------ users and tokens

    /** A user of the main app, as this service sees them: an id and an access token. */
    protected record User(String id, String token) {
    }

    protected User newUser() {
        String id = UUID.randomUUID().toString();
        return new User(id, token(id, "ROLE_CUSTOMER", 900_000));
    }

    /** An access token like the main app's JwtService makes, valid for {@code validForMs} (negative = expired). */
    protected String token(String userId, String role, long validForMs) {
        return token(Map.of("uid", userId, "role", role, "ver", 0), validForMs, jwtSecret);
    }

    protected static String token(Map<String, Object> claims, long validForMs, String base64Secret) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .setClaims(claims)
                .setSubject("user_" + UUID.randomUUID().toString().substring(0, 8))
                .setIssuedAt(new Date(now))
                .setExpiration(new Date(now + validForMs))
                .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(base64Secret)), SignatureAlgorithm.HS256)
                .compact();
    }

    protected String jwtSecret() {
        return jwtSecret;
    }

    // ------------------------------------------------------------------ raw requests

    protected ResponseEntity<String> get(String path, String token) {
        return http.get().uri(path).headers(h -> bearer(h, token)).retrieve().toEntity(String.class);
    }

    protected ResponseEntity<String> post(String path, String token, Object body) {
        return http.post().uri(path)
                .headers(h -> bearer(h, token))
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve().toEntity(String.class);
    }

    protected ResponseEntity<String> patch(String path, String token) {
        return http.patch().uri(path).headers(h -> bearer(h, token)).retrieve().toEntity(String.class);
    }

    private static void bearer(HttpHeaders headers, String token) {
        if (token != null) {
            headers.setBearerAuth(token);
        }
    }

    // ------------------------------------------------------------------ assertions / JSON

    protected static void assertStatus(ResponseEntity<String> response, int expected) {
        assertThat(response.getStatusCode().value())
                .as("HTTP status (body: %s)", response.getBody())
                .isEqualTo(expected);
    }

    protected static JsonNode json(ResponseEntity<String> response) {
        try {
            return JSON.readTree(response.getBody());
        } catch (JsonProcessingException e) {
            throw new UncheckedIOException(e);
        }
    }

    protected static String text(ResponseEntity<String> response, String field) {
        return json(response).get(field).asText();
    }

    /** A made-up 12-digit account number (this service never checks accounts). */
    protected static String accountNumber() {
        return String.valueOf(100_000_000_000L + (long) (Math.random() * 899_999_999_999L));
    }
}
