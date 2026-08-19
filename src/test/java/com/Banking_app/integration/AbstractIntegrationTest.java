package com.Banking_app.integration;

import com.Banking_app.enums.AccountType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Base class for API-level integration tests.
 *
 * <p>These tests exercise the application through its HTTP boundary against a real
 * PostgreSQL instance, so they assert <em>behaviour</em> rather than internal structure.
 * That is deliberate: they are intended to survive the upcoming hexagonal refactor
 * unchanged, and to fail only if the observable behaviour of the API actually changes.
 *
 * <p>The container is started once per JVM (singleton pattern) rather than per class,
 * so the whole suite pays the startup cost only once.
 */
@SpringBootTest
@AutoConfigureMockMvc
public abstract class AbstractIntegrationTest {

    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:17-alpine"));

    static {
        POSTGRES.start();
    }

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * Wipe application data between tests. Flyway's history table is deliberately left
     * alone so migrations are not re-run for every test.
     */
    @BeforeEach
    void resetDatabase() {
        jdbcTemplate.execute(
                "TRUNCATE TABLE ledgers, transactions, accounts, users RESTART IDENTITY CASCADE");
    }

    // ---------------------------------------------------------------------
    // Fixture helpers — these drive the real API rather than touching
    // repositories directly, so setup goes through the same code paths a
    // client would.
    // ---------------------------------------------------------------------

    /** Registers a CUSTOMER through the public endpoint and returns their JWT. */
    protected String registerCustomer(String username, String email, String password) throws Exception {
        String body = json(Map.of(
                "username", username,
                "fullName", username + " Test",
                "email", email,
                "password", password));

        String response = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andReturn()
                .getResponse()
                .getContentAsString();

        return readTree(response).get("accessToken").asText();
    }

    /**
     * Creates the first ADMIN via the bootstrap endpoint and returns their JWT.
     * Only valid while no admin exists, which {@link #resetDatabase()} guarantees.
     */
    protected String bootstrapAdmin(String username, String email, String password) throws Exception {
        String body = json(Map.of(
                "username", username,
                "fullName", username + " Admin",
                "email", email,
                "password", password));

        mockMvc.perform(post("/api/auth/bootstrap-admin/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andReturn();

        return login(username, password);
    }

    protected String login(String username, String password) throws Exception {
        String body = json(Map.of("username", username, "password", password));

        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andReturn()
                .getResponse()
                .getContentAsString();

        return readTree(response).get("accessToken").asText();
    }

    /** Returns the authenticated user's own id, as the API reports it. */
    protected String currentUserId(String token) throws Exception {
        String response = mockMvc.perform(get("/api/auth/me").header("Authorization", bearer(token)))
                .andReturn()
                .getResponse()
                .getContentAsString();

        return readTree(response).get("id").asText();
    }

    /** Admin-only: opens an account for the given user and returns the created account. */
    protected JsonNode createAccount(String adminToken, String userId, AccountType type) throws Exception {
        String body = json(Map.of("userId", userId, "accountType", type.name()));

        String response = mockMvc.perform(post("/api/accounts/create")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andReturn()
                .getResponse()
                .getContentAsString();

        return readTree(response);
    }

    /** Deposits money into an account the caller owns. */
    protected void topUp(String ownerToken, long accountId, String amount) throws Exception {
        mockMvc.perform(patch("/api/accounts/{id}/topup", accountId)
                        .header("Authorization", bearer(ownerToken))
                        .param("amount", amount))
                .andReturn();
    }

    /** Reads an account's current balance straight from the database. */
    protected BigDecimal balanceOf(String accountNumber) {
        return jdbcTemplate.queryForObject(
                "SELECT current_balance FROM accounts WHERE account_number = ?",
                BigDecimal.class,
                accountNumber);
    }

    protected int countLedgerEntries() {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM ledgers", Integer.class);
    }

    protected int countTransactions() {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM transactions", Integer.class);
    }

    /** All ledger rows written for a given transaction reference, oldest first. */
    protected List<Map<String, Object>> ledgerEntriesFor(String reference) {
        return jdbcTemplate.queryForList("""
                SELECT l.ledger_type, l.amount, l.balance_after, a.account_number
                FROM ledgers l
                JOIN transactions t ON t.id = l.transaction_id
                JOIN accounts a ON a.id = l.account_id
                WHERE t.reference_number = ?
                ORDER BY l.id
                """, reference);
    }

    protected void lockAccount(String accountNumber) {
        jdbcTemplate.update(
                "UPDATE accounts SET account_status = 'LOCKED' WHERE account_number = ?", accountNumber);
    }

    protected String bearer(String token) {
        return "Bearer " + token;
    }

    protected String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    protected JsonNode readTree(String value) throws Exception {
        return objectMapper.readTree(value);
    }
}
