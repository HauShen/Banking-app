package com.Banking_app.demo;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.config.CronTask;
import org.springframework.scheduling.config.ScheduledTaskHolder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.RestClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.kafka.KafkaContainer;

import java.math.BigDecimal;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * The whole app started like the hosted demo (plan step 6): demo profile on, Kafka on, and the real
 * ApplicationRunner seeding at startup. Everything else in the demo package tests the parts; this tests that they
 * are wired together and that a visitor gets what the tour promises.
 *
 * <p>Deliberately <b>not</b> on {@code AbstractIntegrationTest}: that shares one PostgreSQL container with the whole
 * suite, and the demo seeded here is committed. Left in the shared database, it would make DemoDataSeederTest skip
 * seeding. So this class starts its own PostgreSQL and its own Kafka (the native image starts in about a second),
 * and {@code @DirtiesContext} closes its context afterwards, so its outbox relay and alert consumer stop.
 *
 * <p>The notification-service is a separate app and not started here; the bell is covered by its own tests.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("demo")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class DemoProfileStartupTest {

    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17");
    static final KafkaContainer KAFKA = new KafkaContainer("apache/kafka-native:4.3.1");

    static {
        POSTGRES.start();   // both stopped automatically by Testcontainers when the JVM exits
        KAFKA.start();
    }

    @DynamicPropertySource
    static void containers(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
        registry.add("app.kafka.enabled", () -> "true");
        // the test application.properties replaces the main one: restate what the consumers need
        registry.add("spring.kafka.consumer.auto-offset-reset", () -> "earliest");
    }

    /** Money as BigDecimal, not double, so balances compare exactly. */
    private static final ObjectMapper JSON =
            new ObjectMapper().enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);
    /** The public demo logins from application-demo.properties. */
    private static final String CUSTOMER_PASSWORD = "Demo1234!";
    private static final String ADMIN_PASSWORD = "DemoAdmin1!";

    @LocalServerPort private int port;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private DemoDataSeeder seeder;
    @Autowired private ScheduledTaskHolder scheduledTasks;
    @Value("${security.login.unthrottled-usernames}") private String unthrottledUsernames;

    private RestClient http;

    @BeforeEach
    void createHttpClient() {
        http = RestClient.builder()
                .baseUrl("http://localhost:" + port)
                .requestFactory(new JdkClientHttpRequestFactory(
                        HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build()))
                .defaultStatusHandler(HttpStatusCode::isError, (request, response) -> { })
                .build();
    }

    @Test
    void startup_seededTheWholeCast() {
        assertThat(count("SELECT COUNT(*) FROM users WHERE email LIKE '%@banking.demo'")).isEqualTo(6);
        for (DemoAccount account : DemoCast.standard(90).accounts()) {
            assertThat(count("SELECT COUNT(*) FROM accounts WHERE account_number = ?", account.accountNumber()))
                    .as(account.accountNumber()).isEqualTo(1);
        }
        assertThat(count("SELECT COUNT(*) FROM transactions")).isGreaterThan(150);
    }

    @Test
    void theWarmUpTransactions_areTheNewest_andTheOnlyOnesSentToKafka() {
        List<String> newest = jdbc.queryForList("""
                SELECT description FROM (
                    SELECT description, created_at, id FROM transactions ORDER BY created_at DESC, id DESC LIMIT 4
                ) last_four ORDER BY created_at, id""", String.class);

        assertThat(newest).containsExactly(
                "Dinner split", "Grab ride share", "Foreign remittance from Lee Chong", "Car down payment");
        assertThat(count("SELECT COUNT(*) FROM outbox_events")).as("history never reaches Kafka").isEqualTo(4);
    }

    @Test
    void weiming_logsInWithTheDemoPassword_andHisChartHasAShape() {
        String token = login("weiming", CUSTOMER_PASSWORD);

        ResponseEntity<String> response = http.get()
                .uri("/api/transfers/account/{n}/balance-history?days=90", DemoCast.MAIN_SAVINGS)
                .headers(h -> h.setBearerAuth(token))
                .retrieve().toEntity(String.class);

        assertThat(response.getStatusCode().value()).as(response.getBody()).isEqualTo(200);
        List<BigDecimal> closing = new ArrayList<>();
        json(response).get("days").forEach(day -> closing.add(day.get("closingBalance").decimalValue()));

        assertThat(closing).hasSize(90);
        assertThat(closing.stream().max(BigDecimal::compareTo).orElseThrow())
                .isGreaterThan(closing.stream().min(BigDecimal::compareTo).orElseThrow());   // not a flat line
        BigDecimal balance = jdbc.queryForObject("SELECT current_balance FROM accounts WHERE account_number = ?",
                BigDecimal.class, DemoCast.MAIN_SAVINGS);
        assertThat(closing.get(89)).isEqualByComparingTo(balance);   // today's point = the balance on screen
    }

    @Test
    void demoadmin_seesTheLargeTransferAlert_raisedThroughKafka() {
        // outbox → relay → Kafka → alert consumer → alert row, all asynchronous: wait for it.
        String reference = jdbc.queryForObject("SELECT reference_number FROM transactions WHERE idempotency_key = ?",
                String.class, DemoWarmUp.idempotencyKey(4));
        String token = login("demoadmin", ADMIN_PASSWORD);

        await().atMost(Duration.ofSeconds(60)).untilAsserted(() -> {
            ResponseEntity<String> response = http.get().uri("/api/admin/alerts?size=100")
                    .headers(h -> h.setBearerAuth(token))
                    .retrieve().toEntity(String.class);
            assertThat(response.getStatusCode().value()).as(response.getBody()).isEqualTo(200);
            List<JsonNode> alerts = new ArrayList<>();
            json(response).get("content").forEach(alerts::add);
            assertThat(alerts).anySatisfy(alert -> {
                assertThat(alert.get("rule").asText()).isEqualTo("LARGE_TRANSFER");
                assertThat(alert.get("reference").asText()).isEqualTo(reference);
            });
        });
    }

    @Test
    void swaggerUi_isOnInTheDemo() {
        ResponseEntity<String> response = http.get().uri("/v3/api-docs").retrieve().toEntity(String.class);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
    }

    @Test
    void aRestart_doesNotSeedAgain() {
        long transactions = count("SELECT COUNT(*) FROM transactions");

        assertThat(seeder.seedIfEmpty()).isEmpty();
        assertThat(count("SELECT COUNT(*) FROM transactions")).isEqualTo(transactions);
    }

    @Test
    void theLoginPage_getsTheDemoLogins_withoutLoggingIn() {
        ResponseEntity<String> response = http.get().uri("/api/demo/logins").retrieve().toEntity(String.class);

        assertThat(response.getStatusCode().value()).as(response.getBody()).isEqualTo(200);
        JsonNode body = json(response);
        assertThat(body.at("/customer/username").asText()).isEqualTo("weiming");
        assertThat(body.at("/customer/password").asText()).isEqualTo(CUSTOMER_PASSWORD);
        assertThat(body.at("/admin/username").asText()).isEqualTo("demoadmin");
        assertThat(body.at("/admin/password").asText()).isEqualTo(ADMIN_PASSWORD);
        List<String> others = new ArrayList<>();
        body.get("otherCustomers").forEach(name -> others.add(name.asText()));
        assertThat(others).containsExactly("mei.ling", "ahmad.f", "priya.n", "jason.k");
        assertThat(body.get("resetTime").asText()).isEqualTo("04:00");
        assertThat(body.get("timeZone").asText()).isEqualTo("Asia/Kuala_Lumpur");

        // and the logins it shows really work
        login(body.at("/customer/username").asText(), body.at("/customer/password").asText());
        login(body.at("/admin/username").asText(), body.at("/admin/password").asText());
    }

    @Test
    void theNightlyReset_isScheduled() {
        // The reset is the app's only cron job (the outbox jobs run at fixed intervals).
        assertThat(scheduledTasks.getScheduledTasks()).anySatisfy(task -> {
            assertThat(task.getTask()).isInstanceOf(CronTask.class);
            assertThat(((CronTask) task.getTask()).getExpression()).isEqualTo("0 0 4 * * *");
        });
    }

    // --- The shared logins can't be locked (plan step C2) ---

    @Test
    void wrongPasswords_cantLockASharedLogin() {
        // Test settings: 5 failures lock a username for 2 s. The demo exempts its logins from that lock.
        for (int i = 0; i < 6; i++) {
            ResponseEntity<String> response = http.post().uri("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("username", "weiming", "password", "wrong-password"))
                    .retrieve().toEntity(String.class);
            assertThat(response.getStatusCode().value()).as(response.getBody()).isEqualTo(401);
        }

        login("weiming", CUSTOMER_PASSWORD);   // 200, not 429
    }

    @Test
    void theLockoutExemption_coversExactlyTheSeededLogins() {
        Set<String> seeded = DemoCast.standard(90).users().stream().map(DemoUser::username)
                .collect(Collectors.toSet());

        assertThat(Set.of(unthrottledUsernames.split(","))).isEqualTo(seeded);
    }

    // --- The demo guard (plan step C1): the seeded users and accounts are shared, so they can't be changed. ---
    // Only requests that write no transactions here: other tests check that the warm-up's 4 are the newest ones.

    @Test
    void theGuard_stopsTheAdmin_fromChangingSeededUsers() {
        String admin = login("demoadmin", ADMIN_PASSWORD);
        DemoCast cast = DemoCast.standard(90);
        String weiming = cast.user("weiming").id();
        Map<String, String> rename = Map.of("fullName", "Renamed", "email", "renamed@example.com");

        assertShared(send(HttpMethod.PUT, "/api/admin/users/" + weiming, admin, rename));
        assertShared(send(HttpMethod.PATCH, "/api/admin/users/" + weiming + "/role", admin, Map.of("role", "ADMIN")));
        assertShared(send(HttpMethod.DELETE, "/api/admin/users/" + weiming, admin, null));
        assertShared(send(HttpMethod.POST, "/api/admin/users/" + weiming + "/revoke-tokens", admin, null));
        assertShared(send(HttpMethod.PUT, "/api/admin/users/" + cast.user("demoadmin").id(), admin, rename));

        assertThat(jdbc.queryForMap("SELECT full_name, role FROM users WHERE username = 'weiming'"))
                .containsEntry("full_name", "Lim Wei Ming")
                .containsEntry("role", "CUSTOMER");
    }

    @Test
    void theGuard_keepsTheSharedLogins_asTheLoginPageShowsThem() {
        String weiming = login("weiming", CUSTOMER_PASSWORD);
        String admin = login("demoadmin", ADMIN_PASSWORD);

        assertShared(send(HttpMethod.PUT, "/api/users/me", weiming,
                Map.of("fullName", "Renamed", "email", "renamed@example.com")));
        assertShared(send(HttpMethod.POST, "/api/auth/change-password", weiming,
                Map.of("currentPassword", CUSTOMER_PASSWORD, "newPassword", "Changed1234!")));
        assertShared(send(HttpMethod.POST, "/api/auth/change-password", admin,
                Map.of("currentPassword", ADMIN_PASSWORD, "newPassword", "Changed1234!")));

        login("weiming", CUSTOMER_PASSWORD);   // the published passwords still work
        login("demoadmin", ADMIN_PASSWORD);
    }

    @Test
    void theGuard_stopsSeededAccounts_fromBeingLockedUnlockedOrDeleted() {
        String admin = login("demoadmin", ADMIN_PASSWORD);
        long savings = accountId(DemoCast.MAIN_SAVINGS);
        long locked = accountId(DemoCast.AHMAD_SAVINGS_LOCKED);

        assertShared(send(HttpMethod.PATCH, "/api/accounts/" + savings + "/status?status=LOCKED", admin, null));
        assertShared(send(HttpMethod.PATCH, "/api/accounts/" + locked + "/status?status=ACTIVE", admin, null));
        assertShared(send(HttpMethod.DELETE, "/api/accounts/" + savings, admin, null));

        assertThat(accountStatus(DemoCast.MAIN_SAVINGS)).isEqualTo("ACTIVE");
        assertThat(accountStatus(DemoCast.AHMAD_SAVINGS_LOCKED)).isEqualTo("LOCKED");
    }

    @Test
    void theGuard_letsVisitorsChange_whatTheyCreated() {
        String admin = login("demoadmin", ADMIN_PASSWORD);
        ResponseEntity<String> created = send(HttpMethod.POST, "/api/admin/users/customers", admin, Map.of(
                "username", "guard.visitor", "fullName", "Guard Visitor",
                "email", "guard.visitor@example.com", "password", "Visitor1234!"));
        assertSuccess(created);
        String id = json(created).get("id").asText();

        // the visitor's own customer can change their profile and password
        String visitor = login("guard.visitor", "Visitor1234!");
        assertSuccess(send(HttpMethod.PUT, "/api/users/me", visitor,
                Map.of("fullName", "Guard Visitor Renamed", "email", "guard.visitor@example.com")));
        assertSuccess(send(HttpMethod.POST, "/api/auth/change-password", visitor,
                Map.of("currentPassword", "Visitor1234!", "newPassword", "Visitor5678!")));

        // and the admin can do everything to them and their account
        assertSuccess(send(HttpMethod.PUT, "/api/admin/users/" + id, admin,
                Map.of("fullName", "Edited By Admin", "email", "guard.visitor@example.com")));
        ResponseEntity<String> account = send(HttpMethod.POST, "/api/accounts/create", admin,
                Map.of("userId", id, "accountType", "SAVINGS"));
        assertSuccess(account);
        long accountId = json(account).get("accountId").asLong();
        assertSuccess(send(HttpMethod.PATCH, "/api/accounts/" + accountId + "/status?status=LOCKED", admin, null));
        assertSuccess(send(HttpMethod.DELETE, "/api/accounts/" + accountId, admin, null));
        assertSuccess(send(HttpMethod.PATCH, "/api/admin/users/" + id + "/role", admin, Map.of("role", "ADMIN")));
        assertSuccess(send(HttpMethod.PATCH, "/api/admin/users/" + id + "/role", admin, Map.of("role", "CUSTOMER")));
        assertSuccess(send(HttpMethod.POST, "/api/admin/users/" + id + "/revoke-tokens", admin, null));
        assertSuccess(send(HttpMethod.DELETE, "/api/admin/users/" + id, admin, null));

        assertThat(count("SELECT COUNT(*) FROM users WHERE username = 'guard.visitor'")).isZero();
    }

    private ResponseEntity<String> send(HttpMethod method, String uri, String token, Object body) {
        RestClient.RequestBodySpec request = http.method(method).uri(uri).headers(h -> h.setBearerAuth(token));
        if (body != null) {
            request.contentType(MediaType.APPLICATION_JSON).body(body);
        }
        return request.retrieve().toEntity(String.class);
    }

    private static void assertShared(ResponseEntity<String> response) {
        assertThat(response.getStatusCode().value()).as(response.getBody()).isEqualTo(403);
        assertThat(json(response).get("message").asText()).startsWith("This is a shared demo");
    }

    private static void assertSuccess(ResponseEntity<String> response) {
        assertThat(response.getStatusCode().is2xxSuccessful()).as(response.getStatusCode() + " " + response.getBody())
                .isTrue();
    }

    private long accountId(String accountNumber) {
        Long id = jdbc.queryForObject("SELECT id FROM accounts WHERE account_number = ?", Long.class, accountNumber);
        return id == null ? 0 : id;
    }

    private String accountStatus(String accountNumber) {
        return jdbc.queryForObject("SELECT account_status FROM accounts WHERE account_number = ?", String.class,
                accountNumber);
    }

    private String login(String username, String password) {
        ResponseEntity<String> response = http.post().uri("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("username", username, "password", password))
                .retrieve().toEntity(String.class);
        assertThat(response.getStatusCode().value()).as(response.getBody()).isEqualTo(200);
        return json(response).get("accessToken").asText();
    }

    private long count(String sql, Object... args) {
        Long n = jdbc.queryForObject(sql, Long.class, args);
        return n == null ? 0 : n;
    }

    private static JsonNode json(ResponseEntity<String> response) {
        try {
            return JSON.readTree(response.getBody());
        } catch (Exception e) {
            throw new IllegalStateException("Not JSON: " + response.getBody(), e);
        }
    }
}
