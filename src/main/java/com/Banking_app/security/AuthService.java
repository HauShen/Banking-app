package com.Banking_app.security;
import com.Banking_app.security.refresh.InvalidRefreshTokenException;
import com.Banking_app.security.refresh.RefreshTokenService;
import com.Banking_app.security.token.TokenVersionService;
import com.Banking_app.userProfile.adapter.out.security.SecurityUserDetails;
import com.Banking_app.userProfile.application.port.in.command.RegisterUserProfileCommand;
import com.Banking_app.userProfile.application.port.in.usecase.RegisterUserProfileUseCase;
import com.Banking_app.userProfile.application.port.out.LoadUserProfilePort;
import com.Banking_app.userProfile.domain.models.userprofile.UserProfile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.*;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;

@Service
public class AuthService  {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RegisterUserProfileUseCase registerUserProfileUseCase;
    private final LoadUserProfilePort loadUserProfilePort;
    private final LoginAttemptService loginAttemptService;
    private final TokenVersionService tokenVersionService;
    private final RefreshTokenService refreshTokenService;

    @Autowired
    public AuthService(@Lazy AuthenticationManager authenticationManager, JwtService jwtService, RegisterUserProfileUseCase registerUserProfileUseCase, LoadUserProfilePort loadUserProfilePort, LoginAttemptService loginAttemptService, TokenVersionService tokenVersionService, RefreshTokenService refreshTokenService){
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.registerUserProfileUseCase = registerUserProfileUseCase;
        this.loadUserProfilePort = loadUserProfilePort;
        this.loginAttemptService = loginAttemptService;
        this.tokenVersionService = tokenVersionService;
        this.refreshTokenService = refreshTokenService;
    }
    public AuthTokens register(AuthenticationRequest request) {
        UserProfile registered = registerUserProfileUseCase.register(new RegisterUserProfileCommand(
                request.getUsername(), request.getFullName(), request.getEmail(), request.getPassword()));

        return startSession(registered);
    }

    /**
     * @param clientIp the caller's IP (request.getRemoteAddr()), used for the per-IP failure limit
     * @throws TooManyLoginAttemptsException if the username or the IP is locked (checked before the password,
     *                                       so a locked account can't be brute-forced in the meantime)
     */
    public AuthTokens login(LoginRequest request, String clientIp) {
        String username = request.getUsername();
        loginAttemptService.blockedFor(username, clientIp).ifPresent(wait -> {
            throw new TooManyLoginAttemptsException(wait);
        });

        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
                    username, request.getPassword()));
        } catch (AuthenticationException e) {   // wrong password or unknown user (both BadCredentialsException)
            loginAttemptService.recordFailure(username, clientIp);
            throw e;
        }
        loginAttemptService.recordSuccess(username);

        UserProfile user = loadUserProfilePort.findByUsername(request.getUsername())
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

        return startSession(user);
    }

    /**
     * Trades a refresh token for a new access token and a new refresh token (rotation).
     * Works without a valid access token - that's the point: the access token may have expired.
     */
    public AuthTokens refresh(String rawRefreshToken) {
        RefreshTokenService.Rotation rotation = refreshTokenService.rotate(rawRefreshToken);
        UserProfile user = loadUserProfilePort.findById(rotation.userId())
                .orElseThrow(() -> new InvalidRefreshTokenException("User no longer exists"));
        return new AuthTokens(accessTokenFor(user), rotation.refreshToken());
    }

    /** Logs the user out on every device. */
    public void logout(String userId) {
        endAllSessions(userId);
    }

    /**
     * All access tokens stop working (token version + 1) and no refresh token can be used any more.
     * Used by logout and by the admin revoke-tokens action.
     */
    public void endAllSessions(String userId) {
        tokenVersionService.revokeAllTokens(userId);
        refreshTokenService.revokeAllForUser(userId);
    }

    private AuthTokens startSession(UserProfile user) {
        return new AuthTokens(accessTokenFor(user), refreshTokenService.issueNewFamily(user.getId()));
    }

    private AuthenticationResponse accessTokenFor(UserProfile user) {
        int tokenVersion = tokenVersionService.currentVersion(user.getId());
        String jwt = jwtService.generateToken(new SecurityUserDetails(user), tokenVersion);
        return new AuthenticationResponse(jwt, "Bearer", jwtService.getExpirationMs() / 1000);
    }
}
