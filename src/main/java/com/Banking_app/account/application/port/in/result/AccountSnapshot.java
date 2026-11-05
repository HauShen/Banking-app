package com.Banking_app.account.application.port.in.result;

import com.Banking_app.account.domain.enums.AccountCurrency;
import com.Banking_app.account.domain.enums.AccountStatus;
import com.Banking_app.account.domain.models.account.Account;

import java.math.BigDecimal;

/**
 * Read-only copy of an account, for other modules (the transaction module) that call the internal use cases.
 * A record instead of the domain Account, so callers outside this module can't change an account.
 */
public record AccountSnapshot(Long id,
                              String accountNumber,
                              String ownerId,
                              AccountStatus status,
                              AccountCurrency currency,
                              BigDecimal balance) {

    public static AccountSnapshot of(Account account) {
        return new AccountSnapshot(account.getId(), account.getAccountNumber(), account.getOwnerId(),
                account.getStatus(), account.getCurrency(), account.getBalance());
    }

    public boolean isLocked() {
        return status == AccountStatus.LOCKED;
    }
}
