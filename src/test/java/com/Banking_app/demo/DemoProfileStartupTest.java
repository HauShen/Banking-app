package com.Banking_app.demo;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
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
