package com.Banking_app.account.adapter.in.web.dto;

import com.Banking_app.account.domain.enums.AccountType;
import jakarta.validation.constraints.NotNull;

/** POST /api/accounts/create — same JSON as before: {"userId": "...", "accountType": "SAVINGS"}. */
public record AccountRequestBody(
        @NotNull(message = "userId is required") String userId,
        @NotNull(message = "accountType is required") AccountType accountType // SAVINGS or CURRENT
) {
}
