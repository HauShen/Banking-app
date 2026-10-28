package com.Banking_app.account.adapter.in.web.mapper;

import com.Banking_app.account.adapter.in.web.dto.AccountResponseBody;
import com.Banking_app.account.adapter.in.web.dto.AccountSummaryResponseBody;
import com.Banking_app.account.application.port.in.usecase.GetAccountSummaryUseCase.AccountSummary;
import com.Banking_app.account.domain.models.account.Account;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AccountWebMapper {

    public AccountResponseBody toResponse(Account account) {
        return new AccountResponseBody(
                account.getId(),
                account.getOwnerId(),
                account.getAccountNumber(),
                account.getType(),
                account.getStatus(),
                account.getCurrency(),
                account.getBalance(),
                account.getCreatedAt()
        );
    }

    public List<AccountResponseBody> toResponses(List<Account> accounts) {
        return accounts.stream().map(this::toResponse).toList();
    }

    public AccountSummaryResponseBody toSummaryResponse(AccountSummary summary) {
        List<AccountResponseBody> accounts = toResponses(summary.accounts());
        // Same rule as the old controller: type of the first account, or "N/A" when there are none.
        String accountType = accounts.isEmpty() ? "N/A" : accounts.get(0).accountType().name();
        return new AccountSummaryResponseBody(summary.totalBalance().doubleValue(), accountType, accounts);
    }
}
