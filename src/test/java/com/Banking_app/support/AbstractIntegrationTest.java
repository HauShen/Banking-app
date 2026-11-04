package com.Banking_app.support;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
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
import java.math.BigDecimal;
import java.net.http.HttpClient;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Base class for HTTP-level integration tests.
 *
 * <ul>
 *   <li>Real embedded Tomcat on a random port, not MockMvc: requests go through the real servlet container,
 *       including the internal forward to /error that once turned a 403 into a 401.</li>
 *   <li>Real PostgreSQL 17 in Docker (Testcontainers), the same engine as production. Flyway runs every
 *       migration, and ddl-auto=validate checks the entities against the resulting schema.</li>
 *   <li>One container for the whole run (singleton pattern), so all test classes share one Spring context.
 *       Tests never assume an empty database: each test creates its own uniquely named users.</li>
 * </ul>
 *
 * The tests only talk HTTP, so they keep working unchanged when the code behind the endpoints is
 * refactored (e.g. Transaction/Ledger to hexagonal) - that is their main job.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class AbstractIntegrationTest {

    /** Created at startup by BootstrapAdminRunner from src/test/resources/application.properties. */
    protected static final String ADMIN_USERNAME = "testadmin";
    protected static final String ADMIN_PASSWORD = "TestAdmin123!";

    /** Password for every customer the tests register. */
    protected static final String PASSWORD = "Passw0rd123";

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

    protected RestClient http;

    @BeforeEach
    void createHttpClient() {
        http = RestClient.builder()
                .baseUrl("http://localhost:" + port)
                // JDK HttpClient over HTTP/1.1: handles a 401 answer to a POST cleanly.
                .requestFactory(new JdkClientHttpRequestFactory(
                        HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build()))
                // Never throw on 4xx/5xx: the tests assert on the status code themselves.
                .defaultStatusHandler(HttpStatusCode::isError, (request, response) -> { })
                .build();
    }

    // ------------------------------------------------------------------ raw requests

    protected ResponseEntity<String> get(String path, String token) {
        return http.get().uri(path)
                .headers(h -> bearer(h, token))
                .retrieve().toEntity(String.class);
    }

    protected ResponseEntity<String> post(String path, String token, Object body) {
        return http.post().uri(path)
                .headers(h -> bearer(h, token))
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve().toEntity(String.class);
    }

    protected ResponseEntity<String> patch(String path, String token) {
        return http.patch().uri(path)
                .headers(h -> bearer(h, token))
                .retrieve().toEntity(String.class);
    }

    protected ResponseEntity<String> delete(String path, String token) {
        return http.delete().uri(path)
                .headers(h -> bearer(h, token))
                .retrieve().toEntity(String.class);
    }

    private static void bearer(HttpHeaders headers, String token) {
        if (token != null) {
            headers.setBearerAuth(token);
        }
    }

    // ------------------------------------------------------------------ assertions / JSON

    /** Asserts the status code and prints the response body if it does not match. */
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

    protected static String uniqueName(String prefix) {
        return prefix + "_" + UUID.randomUUID().toString().substring(0, 8);
    }

    // ------------------------------------------------------------------ scenario helpers

    /** Registers a new customer through the public endpoint and returns their access token. */
    protected String register(String username) {
        ResponseEntity<String> response = post("/api/auth/register", null, Map.of(
                "username", username,
                "fullName", "Test " + username,
                "email", username + "@example.com",
                "password", PASSWORD));
        assertStatus(response, 201);
        return text(response, "accessToken");
    }

    protected String login(String username, String password) {
        ResponseEntity<String> response = post("/api/auth/login", null,
                Map.of("username", username, "password", password));
        assertStatus(response, 200);
        return text(response, "accessToken");
    }

    protected String adminToken() {
        return login(ADMIN_USERNAME, ADMIN_PASSWORD);
    }

    protected String userIdOf(String token) {
        ResponseEntity<String> response = get("/api/users/me", token);
        assertStatus(response, 200);
        return text(response, "id");
    }

    /** Opens a SAVINGS account (RM20 opening balance) for the owner of {@code ownerToken}; admin only. */
    protected Account openAccount(String ownerToken) {
        ResponseEntity<String> response = post("/api/accounts/create", adminToken(),
                Map.of("userId", userIdOf(ownerToken), "accountType", "SAVINGS"));
        assertStatus(response, 201);
        JsonNode body = json(response);
        return new Account(body.get("accountId").asLong(), body.get("accountNumber").asText());
    }

    protected void topUp(String ownerToken, Account account, String amount) {
        assertStatus(patch("/api/accounts/" + account.id() + "/topup?amount=" + amount, ownerToken), 200);
    }

    /** Current balance, read as an admin so it works for any account. */
    protected BigDecimal balanceOf(Account account) {
        ResponseEntity<String> response = get("/api/accounts/" + account.id(), adminToken());
        assertStatus(response, 200);
        return json(response).get("balance").decimalValue();
    }

    protected ResponseEntity<String> transfer(String token, Account from, Account to, String amount,
                                              String idempotencyKey) {
        return post("/api/transfers", token, Map.of(
                "fromAccountNumber", from.number(),
                "toAccountNumber", to.number(),
                "amount", new BigDecimal(amount),
                "currency", "MYR",
                "description", "integration test transfer",
                "idempotencyKey", idempotencyKey));
    }

    /** A registered customer with one account. */
    protected Customer newCustomer(String prefix) {
        String username = uniqueName(prefix);
        String token = register(username);
        return new Customer(username, token, openAccount(token));
    }

    protected record Account(long id, String number) { }

    protected record Customer(String username, String token, Account account) { }
}
