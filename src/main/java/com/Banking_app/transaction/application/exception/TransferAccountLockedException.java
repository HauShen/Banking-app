package com.Banking_app.transaction.application.exception;

/**
 * One side of a transfer is LOCKED. 400 via IllegalArgumentException, with exactly the old messages
 * (including the "an locked" typo: the refactor changes no messages; fix it later as a behaviour change).
 */
public class TransferAccountLockedException extends IllegalArgumentException {

    private TransferAccountLockedException(String message) {
        super(message);
    }

    public static TransferAccountLockedException from() {
        return new TransferAccountLockedException("Cannot process transfer with an locked account.");
    }

    public static TransferAccountLockedException to() {
        return new TransferAccountLockedException("Selected account has been locked");
    }
}
