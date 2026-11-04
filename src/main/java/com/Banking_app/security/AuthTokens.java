package com.Banking_app.security;

import com.Banking_app.security.refresh.IssuedRefreshToken;

/** What login, register and refresh produce: the access token (JSON body) and the refresh token (cookie). */
public record AuthTokens(AuthenticationResponse accessToken, IssuedRefreshToken refreshToken) {
}
