package com.Banking_app.notification.demo;

import com.Banking_app.notification.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The reset against the real schema. It runs inside a transaction that is rolled back (PostgreSQL's TRUNCATE is
 * transactional), so the shared test database keeps the other test classes' rows.
 */
class DemoNotificationsResetTest extends AbstractIntegrationTest {

    @Autowired private JdbcTemplate jdbc;
    @Autowired private PlatformTransactionManager transactionManager;

    @Test
    void reset_emptiesEveryTableExceptFlywaysHistory() {
        DemoNotificationsReset reset = new DemoNotificationsReset(jdbc, transactionManager);

        assertThat(reset.tablesToEmpty()).containsExactly("notifications", "processed_events");

        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            status.setRollbackOnly();
            UUID eventId = UUID.randomUUID();
            jdbc.update("INSERT INTO processed_events (consumer, event_id, processed_at) VALUES ('test', ?, now())",
                    eventId);
            jdbc.update("""
                    INSERT INTO notifications (event_id, user_id, type, message, reference, amount, created_at)
                    VALUES (?, ?, 'TRANSFER_RECEIVED', 'You received RM 45.00.', 'TXN-000000000001', 45.00, now())""",
                    eventId, UUID.randomUUID().toString());

            assertThat(reset.reset()).containsExactly("notifications", "processed_events");

            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM notifications", Long.class)).isZero();
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM processed_events", Long.class)).isZero();
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM " + DemoNotificationsReset.FLYWAY_HISTORY,
                    Long.class)).as("migrations stay applied").isPositive();
        });
    }
}
