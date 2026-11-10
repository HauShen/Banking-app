package com.Banking_app.fx.adapter.out.persistence;

import com.Banking_app.fx.domain.models.ExchangeRate;
import com.Banking_app.fx.domain.models.LockedQuote;
import com.Banking_app.support.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Currency;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** The fx persistence adapter against the real schema (V10, ddl-auto=validate); the owner is a real user. */
class LockedQuotePersistenceAdapterTest extends AbstractIntegrationTest {

    @Autowired
    private LockedQuotePersistenceAdapter adapter;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private PlatformTransactionManager transactionManager;

    /** PostgreSQL keeps microseconds. */
    private static final Instant NOW = Instant.now().truncatedTo(ChronoUnit.MICROS);

    private static final ExchangeRate USD = new ExchangeRate(Currency.getInstance("USD"),
            new BigDecimal("4.60000000"), new BigDecimal("4.71234567"), NOW);

    @Test
    @DisplayName("save writes one fx_quotes row with the owner, all 8 rate decimals, the expiry and no used_at")
    void saves() {
        String userId = userIdOf(register(uniqueName("fxq_alice")));
        UUID id = UUID.randomUUID();
        LockedQuote quote = LockedQuote.lock(id, userId, USD, new BigDecimal("100.00"), NOW, Duration.ofSeconds(60));

        adapter.save(quote);

        Map<String, Object> row = jdbc.queryForMap("SELECT * FROM fx_quotes WHERE id = ?", id.toString());
        assertThat(row.get("user_id")).isEqualTo(userId);
        assertThat(row.get("currency")).isEqualTo("USD");
        assertThat((BigDecimal) row.get("amount")).isEqualByComparingTo("100.00");
        assertThat(((BigDecimal) row.get("exchange_rate")).toPlainString()).isEqualTo("4.71234567");
        assertThat((BigDecimal) row.get("myr_amount")).isEqualByComparingTo("471.23");
        assertThat(((Timestamp) row.get("created_at")).toInstant()).isEqualTo(NOW);
        assertThat(((Timestamp) row.get("expires_at")).toInstant()).isEqualTo(NOW.plusSeconds(60));
        assertThat(row.get("used_at")).isNull();
    }

    @Test
    @DisplayName("the owner must be a real user (foreign key)")
    void ownerMustExist() {
        LockedQuote quote = LockedQuote.lock(UUID.randomUUID(), UUID.randomUUID().toString(), USD,
                new BigDecimal("100.00"), NOW, Duration.ofSeconds(60));

        assertThatThrownBy(() -> adapter.save(quote)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("deleting the user deletes their quotes")
    void cascadesOnUserDelete() {
        String token = register(uniqueName("fxq_bob"));
        String userId = userIdOf(token);
        UUID id = UUID.randomUUID();
        adapter.save(LockedQuote.lock(id, userId, USD, new BigDecimal("1.00"), NOW, Duration.ofSeconds(60)));

        assertStatus(delete("/api/admin/users/" + userId, adminToken()), 204);

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM fx_quotes WHERE id = ?", Integer.class, id.toString()))
                .isZero();
    }

    @Test
    @DisplayName("loadForUpdate reads the quote back (JPY 10000 as 10000.00); saving the used quote updates used_at")
    void loadsAndMarksUsed() {
        String userId = userIdOf(register(uniqueName("fxq_carol")));
        ExchangeRate jpy = new ExchangeRate(Currency.getInstance("JPY"), new BigDecimal("0.0290"),
                new BigDecimal("0.0305"), NOW);
        UUID id = UUID.randomUUID();
        adapter.save(LockedQuote.lock(id, userId, jpy, new BigDecimal("10000"), NOW, Duration.ofSeconds(60)));
        Instant usedAt = NOW.plusSeconds(5);

        LockedQuote loaded = new TransactionTemplate(transactionManager).execute(status -> {
            LockedQuote quote = adapter.loadForUpdate(id).orElseThrow();   // FOR UPDATE needs a transaction
            adapter.save(quote.consume(userId, "JPY", new BigDecimal("10000"), usedAt));
            return quote;
        });

        assertThat(loaded.getOwnerId()).isEqualTo(userId);
        assertThat(loaded.getAmount().toPlainString()).isEqualTo("10000.00");
        assertThat(loaded.getExchangeRate()).isEqualByComparingTo("0.0305");
        assertThat(loaded.isUsed()).isFalse();
        Timestamp stored = jdbc.queryForObject("SELECT used_at FROM fx_quotes WHERE id = ?", Timestamp.class,
                id.toString());
        assertThat(stored.toInstant()).isEqualTo(usedAt);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM fx_quotes WHERE id = ?", Integer.class, id.toString()))
                .isEqualTo(1);   // updated, not inserted again
    }

    @Test
    @DisplayName("loadForUpdate of an unknown id → empty")
    void unknown() {
        // A typed variable first: passing execute(...) straight into the overloaded assertThat leaves T ambiguous.
        Optional<LockedQuote> loaded = new TransactionTemplate(transactionManager)
                .execute(status -> adapter.loadForUpdate(UUID.randomUUID()));

        assertThat(loaded).isEmpty();
    }
}
