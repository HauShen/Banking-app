package com.Banking_app.account.application.port.out;

import com.Banking_app.account.domain.models.account.Account;

public interface SaveAccountPort {
    /** Inserts a new account (id == null) or updates an existing one. Returns the saved account with its id. */
    Account save(Account account);
}
