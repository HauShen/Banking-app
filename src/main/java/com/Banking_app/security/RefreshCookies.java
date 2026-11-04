package com.Banking_app.security;

import com.Banking_app.security.refresh.IssuedRefreshToken;
import com.Banking_app.security.refresh.RefreshTokenProperties;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

/**
 * Builds the Set-Cookie header for the refresh token.
 *
 * <ul>
 *   <li>HttpOnly: JavaScript (and so any XSS) can't read it.</li>
 *   <li>Secure: only sent over HTTPS (http://localhost is treated as secure by browsers).</li>
 *   <li>SameSite=Strict: never sent on requests started by another site, which blocks CSRF on /refresh.
 *       The frontend and the API must be on the same site (e.g. app.example.com + api.example.com;
 *       localhost:5173 + localhost:8080 also count as same-site).</li>
 *   <li>Path=/api/auth: not sent with ordinary API calls, only to /api/auth/*.</li>
 * </ul>
 */
@Component
public class RefreshCookies {

    public static final String NAME = "refresh_token";
    static final String PATH = "/api/auth";

    private final RefreshTokenProperties properties;

    public RefreshCookies(RefreshTokenProperties properties) {
        this.properties = properties;
    }

    public String create(IssuedRefreshToken token) {
        return base(token.rawToken()).maxAge(token.lifetime()).build().toString();
    }

    /** Tells the browser to delete the cookie (same name and path, Max-Age=0). */
    public String clear() {
        return base("").maxAge(0).build().toString();
    }

    private ResponseCookie.ResponseCookieBuilder base(String value) {
        return ResponseCookie.from(NAME, value)
                .httpOnly(true)
                .secure(properties.cookieSecure())
                .sameSite("Strict")
                .path(PATH);
    }
}
