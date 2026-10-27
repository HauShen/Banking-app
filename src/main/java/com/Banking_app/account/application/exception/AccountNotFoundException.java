package com.Banking_app.account.application.exception;

import com.Banking_app.exception.ResourceNotFoundException;

/** Account does not exist. ResourceNotFoundException is already mapped to 404 by GlobalExceptionHandler. */
public class AccountNotFoundException extends ResourceNotFoundException {
    public AccountNotFoundException(String message) {
        super(message);
    }
}
