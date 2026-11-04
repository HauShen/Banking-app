package com.Banking_app.security.refresh;

/** Missing, unknown, expired, revoked or reused refresh token. The client only ever sees a generic 401. */
public class InvalidRefreshTokenException extends RuntimeException {
    public InvalidRefreshTokenException(String reason) {
        super(reason);
    }
}
