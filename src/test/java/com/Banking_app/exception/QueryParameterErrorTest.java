package com.Banking_app.exception;

import com.Banking_app.support.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

/** A query parameter of the wrong type, or a missing one, is the client's mistake: 400, never 500. */
class QueryParameterErrorTest extends AbstractIntegrationTest {

    @Test
    @DisplayName("?page=abc on the transaction history → 400 naming the parameter")
    void wrongType() {
        Customer alice = newCustomer("qp_alice");

        ResponseEntity<String> response = get("/api/transfers/account/" + alice.account().number() + "?page=abc",
                alice.token());

        assertStatus(response, 400);
        assertThat(text(response, "message")).isEqualTo("Invalid value for parameter 'page'");
        assertThat(text(response, "path")).isEqualTo("/api/transfers/account/" + alice.account().number());
    }

    @Test
    @DisplayName("a top-up without ?amount → 400 naming the parameter")
    void missing() {
        Customer alice = newCustomer("qp_alice");

        ResponseEntity<String> response = patch("/api/accounts/" + alice.account().id() + "/topup", alice.token());

        assertStatus(response, 400);
        assertThat(text(response, "message")).isEqualTo("Missing parameter 'amount'");
    }
}
