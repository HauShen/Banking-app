package com.Banking_app.account.application.exception;

import com.Banking_app.exception.ResourceNotFoundException;

/** The user the account belongs to (or should belong to) does not exist. Mapped to 404. */
public class AccountOwnerNotFoundException extends ResourceNotFoundException {
    public AccountOwnerNotFoundException(String userId) {
        super("User not found with id: " + userId);
    }
}
