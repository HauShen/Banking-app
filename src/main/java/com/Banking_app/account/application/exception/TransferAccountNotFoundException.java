package com.Banking_app.account.application.exception;

import com.Banking_app.exception.ResourceNotFoundException;

/**
 * One side of a transfer does not exist. 404, with the same messages the old transfer code used
 * ("From account not found" / "To account not found").
 */
public class TransferAccountNotFoundException extends ResourceNotFoundException {

    private TransferAccountNotFoundException(String message) {
        super(message);
    }

    public static TransferAccountNotFoundException from() {
        return new TransferAccountNotFoundException("From account not found");
    }

    public static TransferAccountNotFoundException to() {
        return new TransferAccountNotFoundException("To account not found");
    }
}
