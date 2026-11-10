package com.Banking_app.transaction.adapter.in.web;

import com.Banking_app.support.AbstractIntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * POST /api/transfers with a quoteId from POST /api/fx/quotes, over real HTTP. Test rates: sell USD 4.75, SGD 3.50.
 * The test rates never change, so "the rate moved since the quote was locked" is simulated by editing the quote's
 * row; "expired" by moving its times into the past.
 */
class LockedQuoteTransferTest extends AbstractIntegrationTest {

    @Autowired
    private JdbcTemplate jdbc;

    private static String key() {
        return "quote-transfer-" + UUID.randomUUID();
    }

    private static String message(ResponseEntity<String> response) {
        return json(response).get("message").asText();
    }

    /** Alice with RM 120.00 (20.00 opening + 100.00 top-up) and Bob with RM 20.00. */
    private Customer[] aliceAndBob() {
        Customer alice = newCustomer("lq_alice");
        Customer bob = newCustomer("lq_bob");
        topUp(alice.token(), alice.account(), "100.00");
        return new Customer[]{alice, bob};
    }

    private String lockQuote(Customer customer, String currency, String amount) {
        ResponseEntity<String> response = post("/api/fx/quotes", customer.token(),
                Map.of("currency", currency, "amount", new BigDecimal(amount)));
        assertStatus(response, 201);
        return text(response, "quoteId");
    }

    private ResponseEntity<String> transferWithQuote(Customer from, Customer to, String amount, String currency,
                                                     String quoteId, String key) {
        Map<String, Object> body = new HashMap<>(Map.of(
                "fromAccountNumber", from.account().number(),
                "toAccountNumber", to.account().number(),
                "amount", new BigDecimal(amount),
                "description", "locked quote test",
                "idempotencyKey", key,
                "quoteId", quoteId));
        if (currency != null) {
            body.put("currency", currency);
        }
        return post("/api/transfers", from.token(), body);
    }

    private boolean isUsed(String quoteId) {
        return jdbc.queryForObject("SELECT used_at IS NOT NULL FROM fx_quotes WHERE id = ?", Boolean.class, quoteId);
    }

    @Nested
    @DisplayName("paying at a locked rate")
    class Paying {

        @Test
        @DisplayName("lock 20.00 USD, then transfer with the quote → RM 95.00 moves, the quote is used")
        void usesQuote() {
            Customer[] p = aliceAndBob();
            String quoteId = lockQuote(p[0], "USD", "20.00");

            ResponseEntity<String> response = transferWithQuote(p[0], p[1], "20.00", "USD", quoteId, key());

            assertStatus(response, 201);
            assertThat(json(response).get("amount").decimalValue()).isEqualByComparingTo("95.00");
            assertThat(json(response).get("exchangeRate").decimalValue()).isEqualByComparingTo("4.75");
            assertThat(balanceOf(p[0].account())).isEqualByComparingTo("25.00");
            assertThat(isUsed(quoteId)).isTrue();
        }

        @Test
        @DisplayName("the LOCKED rate is booked even when today's differs (quote locked at 4.70, today 4.75)")
        void lockedRateWins() {
            Customer[] p = aliceAndBob();
            String quoteId = lockQuote(p[0], "USD", "20.00");
            jdbc.update("UPDATE fx_quotes SET exchange_rate = 4.70, myr_amount = 94.00 WHERE id = ?", quoteId);

            ResponseEntity<String> response = transferWithQuote(p[0], p[1], "20.00", "USD", quoteId, key());

            assertStatus(response, 201);
            assertThat(json(response).get("amount").decimalValue()).isEqualByComparingTo("94.00");
            assertThat(json(response).get("exchangeRate").decimalValue()).isEqualByComparingTo("4.70");
            JsonNode tx = json(get("/api/transfers/" + text(response, "transactionReference"), p[0].token()));
            assertThat(tx.get("exchangeRate").decimalValue()).isEqualByComparingTo("4.70");
            assertThat(tx.get("amount").decimalValue()).isEqualByComparingTo("94.00");
            assertThat(balanceOf(p[1].account())).isEqualByComparingTo("114.00");
        }

        @Test
        @DisplayName("a failed transfer (minimum balance) leaves the quote unused; after a top-up it still works")
        void failedTransferKeepsQuote() {
            Customer alice = newCustomer("lq_alice");   // RM 20.00: nothing to spare
            Customer bob = newCustomer("lq_bob");
            String quoteId = lockQuote(alice, "USD", "1.00");

            ResponseEntity<String> failed = transferWithQuote(alice, bob, "1.00", "USD", quoteId, key());
            assertStatus(failed, 400);
            assertThat(message(failed)).isEqualTo("Insufficient balance. Minimum balance of $20.00 must be maintained.");
            assertThat(isUsed(quoteId)).isFalse();   // rolled back with the transfer

            topUp(alice.token(), alice.account(), "10.00");
            assertStatus(transferWithQuote(alice, bob, "1.00", "USD", quoteId, key()), 201);
            assertThat(isUsed(quoteId)).isTrue();
        }

        @Test
        @DisplayName("a retry with the same idempotency key after the quote was used → the original transfer")
        void retry() {
            Customer[] p = aliceAndBob();
            String quoteId = lockQuote(p[0], "USD", "20.00");
            String key = key();
            String reference = text(transferWithQuote(p[0], p[1], "20.00", "USD", quoteId, key), "transactionReference");

            ResponseEntity<String> retry = transferWithQuote(p[0], p[1], "20.00", "USD", quoteId, key);

            assertStatus(retry, 201);
            assertThat(text(retry, "transactionReference")).isEqualTo(reference);
            assertThat(text(retry, "message")).isEqualTo("Duplicate request detected. Returning existing transaction.");
            assertThat(balanceOf(p[0].account())).isEqualByComparingTo("25.00");
        }
    }

    @Nested
    @DisplayName("refused (400, nothing moves)")
    class Refused {

        @Test
        @DisplayName("the quote used a second time, by another transfer → 'The quote has already been used'")
        void usedTwice() {
            Customer[] p = aliceAndBob();
            String quoteId = lockQuote(p[0], "USD", "10.00");
            assertStatus(transferWithQuote(p[0], p[1], "10.00", "USD", quoteId, key()), 201);

            ResponseEntity<String> second = transferWithQuote(p[0], p[1], "10.00", "USD", quoteId, key());

            assertStatus(second, 400);
            assertThat(message(second)).isEqualTo("The quote has already been used");
            assertThat(balanceOf(p[0].account())).isEqualByComparingTo("72.50");   // 120 - 47.50 once
        }

        @Test
        @DisplayName("expired → 'The quote has expired'")
        void expired() {
            Customer[] p = aliceAndBob();
            String quoteId = lockQuote(p[0], "USD", "20.00");
            jdbc.update("UPDATE fx_quotes SET created_at = NOW() - INTERVAL '2 minutes', "
                    + "expires_at = NOW() - INTERVAL '1 minute' WHERE id = ?", quoteId);

            ResponseEntity<String> response = transferWithQuote(p[0], p[1], "20.00", "USD", quoteId, key());

            assertStatus(response, 400);
            assertThat(message(response)).isEqualTo("The quote has expired");
            assertThat(balanceOf(p[0].account())).isEqualByComparingTo("120.00");
        }

        @Test
        @DisplayName("someone else's quote and an unknown quote get the same 'Quote not found'")
        void notFound() {
            Customer[] p = aliceAndBob();
            String bobsQuote = lockQuote(p[1], "USD", "20.00");

            ResponseEntity<String> others = transferWithQuote(p[0], p[1], "20.00", "USD", bobsQuote, key());
            ResponseEntity<String> unknown = transferWithQuote(p[0], p[1], "20.00", "USD",
                    UUID.randomUUID().toString(), key());

            assertStatus(others, 400);
            assertThat(message(others)).isEqualTo("Quote not found");
            assertStatus(unknown, 400);
            assertThat(message(unknown)).isEqualTo("Quote not found");
            assertThat(isUsed(bobsQuote)).isFalse();
            assertThat(balanceOf(p[0].account())).isEqualByComparingTo("120.00");
        }

        @Test
        @DisplayName("another amount or currency than the quote's → 'The quote is for a different currency or amount'")
        void differentPayment() {
            Customer[] p = aliceAndBob();
            String quoteId = lockQuote(p[0], "USD", "20.00");

            ResponseEntity<String> otherAmount = transferWithQuote(p[0], p[1], "21.00", "USD", quoteId, key());
            ResponseEntity<String> otherCurrency = transferWithQuote(p[0], p[1], "20.00", "SGD", quoteId, key());

            assertStatus(otherAmount, 400);
            assertThat(message(otherAmount)).isEqualTo("The quote is for a different currency or amount");
            assertStatus(otherCurrency, 400);
            assertThat(message(otherCurrency)).isEqualTo("The quote is for a different currency or amount");
            assertThat(isUsed(quoteId)).isFalse();
        }

        @Test
        @DisplayName("a quote with MYR or without a currency → 'quoteId can only be used with a foreign currency'")
        void withMyr() {
            Customer[] p = aliceAndBob();
            String quoteId = lockQuote(p[0], "USD", "20.00");

            ResponseEntity<String> myr = transferWithQuote(p[0], p[1], "20.00", "MYR", quoteId, key());
            ResponseEntity<String> none = transferWithQuote(p[0], p[1], "20.00", null, quoteId, key());

            assertStatus(myr, 400);
            assertThat(message(myr)).isEqualTo("quoteId can only be used with a foreign currency");
            assertStatus(none, 400);
            assertThat(balanceOf(p[0].account())).isEqualByComparingTo("120.00");
        }

        @Test
        @DisplayName("a quoteId that isn't a UUID → 400 field error")
        void notAUuid() {
            Customer[] p = aliceAndBob();

            ResponseEntity<String> response = transferWithQuote(p[0], p[1], "20.00", "USD", "not-a-uuid", key());

            assertStatus(response, 400);
            assertThat(json(response).get("fieldErrors").get("quoteId").asText()).isEqualTo("quoteId must be a UUID");
        }
    }
}
