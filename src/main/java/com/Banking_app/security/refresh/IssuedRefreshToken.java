package com.Banking_app.security.refresh;

import java.time.Duration;

/** A freshly created refresh token: the raw value goes to the client once (in a cookie) and is never stored. */
public record IssuedRefreshToken(String rawToken, Duration lifetime) {

    /** Never print the token itself, e.g. in a log line. */
    @Override
    public String toString() {
        return "IssuedRefreshToken[lifetime=" + lifetime + "]";
    }
}
