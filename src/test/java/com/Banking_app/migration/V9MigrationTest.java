package com.Banking_app.migration;

import com.Banking_app.support.AbstractIntegrationTest;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * V9 against data that existed BEFORE it: a separate schema is migrated to V8 with Flyway, filled with old-style rows
 * (a transfer and a top-up), then migrated to V9. Checks the backfill of transaction_type and the new constraints.
 *
 * Uses its own connection with currentSchema=v9_migration_test, so the tables every other test uses are untouched.
 */
class V9MigrationTest extends AbstractIntegrationTest {

    private static final String SCHEMA = "v9_migration_test";

    @Value("${spring.datasource.url}")
    private String url;

    @Value("${spring.datasource.username}")
    private String username;

    @Value("${spring.datasource.password}")
    private String password;

    private DriverManagerDataSource dataSource;
    private JdbcTemplate jdbc;

    @BeforeEach
    void freshSchema() {
        String separator = url.contains("?") ? "&" : "?";
        dataSource = new DriverManagerDataSource(url + separator + "currentSchema=" + SCHEMA, username, password);
        jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("DROP SCHEMA IF EXISTS " + SCHEMA + " CASCADE");
    }

    @AfterEach
    void dropSchema() {
        jdbc.execute("DROP SCHEMA IF EXISTS " + SCHEMA + " CASCADE");
    }

    private void migrateTo(String version) {
        Flyway.configure()
                .dataSource(dataSource)
                .schemas(SCHEMA)
                .locations("classpath:db/migration")
                .target(version)
                .load()
                .migrate();
    }

    /** Old rows, written the way the app wrote them before V9: a transfer and a cash deposit (no from-account). */
    private long[] insertPreV9Rows() {
        jdbc.update("""
                INSERT INTO users (id, username, role, full_name, email, password, created_at, updated_at)
                VALUES ('u-1', 'migration_user', 'CUSTOMER', 'Migration User', 'migration@test.local', 'x', NOW(), NOW())
                """);
        Long from = jdbc.queryForObject("""
                INSERT INTO accounts (user_id, account_number, account_type, account_status, current_balance)
                VALUES ('u-1', 'MIG0000001', 'SAVINGS', 'ACTIVE', 100.00) RETURNING id""", Long.class);
        Long to = jdbc.queryForObject("""
                INSERT INTO accounts (user_id, account_number, account_type, account_status, current_balance)
                VALUES ('u-1', 'MIG0000002', 'SAVINGS', 'ACTIVE', 20.00) RETURNING id""", Long.class);
        Long transfer = jdbc.queryForObject("""
                INSERT INTO transactions (reference_number, from_account_id, to_account_id, amount, status, idempotency_key)
                VALUES ('TXN-MIGRATION01', ?, ?, 10.00, 'SUCCESS', 'mig-key-1') RETURNING id""", Long.class, from, to);
        Long deposit = jdbc.queryForObject("""
                INSERT INTO transactions (reference_number, from_account_id, to_account_id, amount, status, idempotency_key)
                VALUES ('CDM-MIGRATION01', NULL, ?, 50.00, 'SUCCESS', 'mig-key-2') RETURNING id""", Long.class, to);
        return new long[]{transfer, deposit, from, to};
    }

    private String typeOf(long transactionId) {
        return jdbc.queryForObject("SELECT transaction_type FROM transactions WHERE id = ?", String.class, transactionId);
    }

    @Test
    @DisplayName("existing rows get their type: with a from-account → TRANSFER, without → CASH_DEPOSIT; FX columns empty")
    void backfillsTransactionType() {
        migrateTo("8");
        long[] ids = insertPreV9Rows();

        migrateTo("9");

        assertThat(typeOf(ids[0])).isEqualTo("TRANSFER");
        assertThat(typeOf(ids[1])).isEqualTo("CASH_DEPOSIT");
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM transactions WHERE original_currency IS NOT NULL OR exchange_rate IS NOT NULL",
                Integer.class)).isZero();
    }

    @Test
    @DisplayName("after V9: the type is required, must match the from-account, and the FX columns are all-or-nothing")
    void newConstraints() {
        migrateTo("8");
        long[] ids = insertPreV9Rows();
        long from = ids[2];
        long to = ids[3];
        migrateTo("9");

        // type is NOT NULL
        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO transactions (reference_number, from_account_id, to_account_id, amount, status, idempotency_key)
                VALUES ('TXN-NOTYPE', ?, ?, 1.00, 'SUCCESS', 'k-notype')""", from, to))
                .isInstanceOf(DataIntegrityViolationException.class);

        // a TRANSFER needs a from-account; a CASH_DEPOSIT must not have one
        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO transactions (reference_number, transaction_type, from_account_id, to_account_id, amount,
                                          status, idempotency_key)
                VALUES ('TXN-NOSENDER', 'TRANSFER', NULL, ?, 1.00, 'SUCCESS', 'k-nosender')""", to))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO transactions (reference_number, transaction_type, from_account_id, to_account_id, amount,
                                          status, idempotency_key)
                VALUES ('CDM-SENDER', 'CASH_DEPOSIT', ?, ?, 1.00, 'SUCCESS', 'k-sender')""", from, to))
                .isInstanceOf(DataIntegrityViolationException.class);

        // unknown type
        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO transactions (reference_number, transaction_type, from_account_id, to_account_id, amount,
                                          status, idempotency_key)
                VALUES ('TXN-BADTYPE', 'REFUND', ?, ?, 1.00, 'SUCCESS', 'k-badtype')""", from, to))
                .isInstanceOf(DataIntegrityViolationException.class);

        // FX columns: only some of them set → refused; all set → fine
        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO transactions (reference_number, transaction_type, from_account_id, to_account_id, amount,
                                          status, idempotency_key, original_currency)
                VALUES ('TXN-HALFFX', 'TRANSFER', ?, ?, 1.00, 'SUCCESS', 'k-halffx', 'USD')""", from, to))
                .isInstanceOf(DataIntegrityViolationException.class);
        jdbc.update("""
                INSERT INTO transactions (reference_number, transaction_type, from_account_id, to_account_id, amount,
                                          status, idempotency_key, original_currency, original_amount, exchange_rate)
                VALUES ('TXN-FULLFX', 'TRANSFER', ?, ?, 4.75, 'SUCCESS', 'k-fullfx', 'USD', 1.00, 4.75000000)""",
                from, to);
        assertThat(jdbc.queryForObject("SELECT exchange_rate FROM transactions WHERE reference_number = 'TXN-FULLFX'",
                BigDecimal.class)).isEqualByComparingTo("4.75");
    }
}
