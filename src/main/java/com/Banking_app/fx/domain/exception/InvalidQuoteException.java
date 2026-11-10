package com.Banking_app.fx.domain.exception;

/**
 * A locked quote that can't be used for this payment. 400 via IllegalArgumentException.
 * Unknown and someone else's quote get the same message, so a quote id reveals nothing about other users.
 */
public class InvalidQuoteException extends IllegalArgumentException {

    private InvalidQuoteException(String message) {
        super(message);
    }

    public static InvalidQuoteException notFound() {
        return new InvalidQuoteException("Quote not found");
    }

    public static InvalidQuoteException expired() {
        return new InvalidQuoteException("The quote has expired");
    }

    public static InvalidQuoteException alreadyUsed() {
        return new InvalidQuoteException("The quote has already been used");
    }

    public static InvalidQuoteException differentPayment() {
        return new InvalidQuoteException("The quote is for a different currency or amount");
    }
}
