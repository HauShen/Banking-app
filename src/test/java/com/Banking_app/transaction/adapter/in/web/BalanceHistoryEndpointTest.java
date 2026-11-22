package com.Banking_app.transaction.adapter.in.web;

import com.Banking_app.support.AbstractIntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * GET /api/transfers/account/{accountNumber}/balance-history over real HTTP. Accounts opened here exist only since
 * today, so the history has one day; how days add up over time is covered by GetBalanceHistoryServiceTest and
 * LedgerPersistenceAdapterTest.
 */
class BalanceHistoryEndpointTest extends AbstractIntegrationTest {

    private static String url(Account account) {
        return "/api/transfers/account/" + account.number() + "/balance-history";
    }

    /** Today where the bank is (bank.time-zone default). */
    private static String today() {
        return LocalDate.now(ZoneId.of("Asia/Kuala_Lumpur")).toString();
    }

    @Test
    @DisplayName("a new account: one day, today, at the RM20 opening balance; currency and time zone included")
    void newAccount() {
        Customer alice = newCustomer("bh_alice");

        ResponseEntity<String> response = get(url(alice.account()), alice.token());

        assertStatus(response, 200);
        JsonNode body = json(response);
        assertThat(body.get("accountNumber").asText()).isEqualTo(alice.account().number());
        assertThat(body.get("currency").asText()).isEqualTo("MYR");
        assertThat(body.get("timeZone").asText()).isEqualTo("Asia/Kuala_Lumpur");
        assertThat(body.get("days")).hasSize(1);
        assertThat(body.get("days").get(0).get("date").asText()).isEqualTo(today());
        assertThat(body.get("days").get(0).get("closingBalance").decimalValue()).isEqualByComparingTo("20.00");
    }

    @Test
    @DisplayName("after a top-up and a transfer, today closes at each account's current balance")
    void followsTheLedger() {
        Customer alice = newCustomer("bh_alice");
        Customer bob = newCustomer("bh_bob");
        topUp(alice.token(), alice.account(), "100.00");
        assertStatus(transfer(alice.token(), alice.account(), bob.account(), "30.00", UUID.randomUUID().toString()),
                201);

        JsonNode alices = json(get(url(alice.account()) + "?days=7", alice.token())).get("days");
        JsonNode bobs = json(get(url(bob.account()), bob.token())).get("days");

        assertThat(alices.get(alices.size() - 1).get("closingBalance").decimalValue())
                .isEqualByComparingTo(balanceOf(alice.account()))
                .isEqualByComparingTo("90.00");
        assertThat(bobs.get(bobs.size() - 1).get("closingBalance").decimalValue()).isEqualByComparingTo("50.00");
    }

    @Test
    @DisplayName("owner and admins only: another customer gets 403, no token 401, an unknown account 404")
    void access() {
        Customer alice = newCustomer("bh_alice");
        Customer eve = newCustomer("bh_eve");

        assertStatus(get(url(alice.account()), adminToken()), 200);
        ResponseEntity<String> forbidden = get(url(alice.account()), eve.token());
        assertStatus(forbidden, 403);
        assertThat(text(forbidden, "message")).isEqualTo("You can only view the balance history of your own accounts");
        assertStatus(get(url(alice.account()), null), 401);
        assertStatus(get("/api/transfers/account/9999999999/balance-history", alice.token()), 404);
    }

    @Test
    @DisplayName("days outside 1..366, or not a number → 400")
    void badDays() {
        Customer alice = newCustomer("bh_alice");

        for (String days : new String[]{"0", "367", "-5"}) {
            ResponseEntity<String> response = get(url(alice.account()) + "?days=" + days, alice.token());
            assertStatus(response, 400);
            assertThat(text(response, "message")).isEqualTo("days must be between 1 and 366");
        }
        ResponseEntity<String> notANumber = get(url(alice.account()) + "?days=week", alice.token());
        assertStatus(notANumber, 400);
        assertThat(text(notANumber, "message")).isEqualTo("Invalid value for parameter 'days'");
    }
}
