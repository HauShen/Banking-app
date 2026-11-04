package com.Banking_app.security;
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

    @Autowired
    public AuthService(@Lazy AuthenticationManager authenticationManager, JwtService jwtService, RegisterUserProfileUseCase registerUserProfileUseCase, LoadUserProfilePort loadUserProfilePort, LoginAttemptService loginAttemptService){
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.registerUserProfileUseCase = registerUserProfileUseCase;
        this.loadUserProfilePort = loadUserProfilePort;
        this.loginAttemptService = loginAttemptService;
    }
    public AuthenticationResponse register(AuthenticationRequest request) {
        UserProfile registered = registerUserProfileUseCase.register(new RegisterUserProfileCommand(
                request.getUsername(), request.getFullName(), request.getEmail(), request.getPassword()));

        String jwt = jwtService.generateToken(new SecurityUserDetails(registered));
        return new AuthenticationResponse(jwt, "Bearer", jwtService.getExpirationMs() / 1000);
    }

    /**
     * @param clientIp the caller's IP (request.getRemoteAddr()), used for the per-IP failure limit
     * @throws TooManyLoginAttemptsException if the username or the IP is locked (checked before the password,
     *                                       so a locked account can't be brute-forced in the meantime)
     */
    public AuthenticationResponse login(LoginRequest request, String clientIp) {
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

        String jwt = jwtService.generateToken(new SecurityUserDetails(user));
        return new AuthenticationResponse(jwt, "Bearer", jwtService.getExpirationMs() / 1000);
    }
}
