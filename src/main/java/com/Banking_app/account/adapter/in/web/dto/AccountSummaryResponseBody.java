package com.Banking_app.account.adapter.in.web.dto;

import java.util.List;

/**
 * GET /api/accounts/summary — same JSON as the old Map: {"balance", "accountType", "accounts"}.
 * balance stays a double for now so the frontend sees no change; step 16 switches it to BigDecimal.
 */
public record AccountSummaryResponseBody(
        double balance,
        String accountType,
        List<AccountResponseBody> accounts
) {
}
