package com.Banking_app.account.adapter.in.web.dto;

import com.Banking_app.account.domain.enums.AccountCurrency;
import com.Banking_app.account.domain.enums.AccountStatus;
import com.Banking_app.account.domain.enums.AccountType;

import java.math.BigDecimal;
import java.time.Instant;

/** Same JSON field names and order as the old dto.responseBodies.AccountResponseBody. */
public record AccountResponseBody(
        Long accountId,
        String userId,
        String accountNumber,
        AccountType accountType,
        AccountStatus accountStatus,
        AccountCurrency accountCurrency,
        BigDecimal balance,
        Instant createdAt
) {
}
