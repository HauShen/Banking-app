package com.Banking_app.account.application.port.in.usecase;

import com.Banking_app.account.domain.models.account.Account;

import java.math.BigDecimal;
import java.util.List;

public interface GetAccountSummaryUseCase {
    AccountSummary getSummary(String ownerId);

    record AccountSummary(BigDecimal totalBalance, List<Account> accounts) {
    }
}
