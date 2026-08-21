package com.Banking_app.security;
import com.Banking_app.userProfile.adapter.out.security.AuthenticationRequest;
import com.Banking_app.userProfile.adapter.out.security.AuthenticationResponse;
import com.Banking_app.userProfile.adapter.out.security.LoginRequest;
import com.Banking_app.userProfile.adapter.out.security.SecurityUserDetails;
import com.Banking_app.userProfile.application.port.in.command.RegisterUserProfileCommand;
import com.Banking_app.userProfile.application.port.in.usecase.RegisterUserProfileUseCase;
import com.Banking_app.userProfile.application.port.out.LoadUserProfilePort;
import com.Banking_app.userProfile.domain.models.userprofile.UserProfile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.*;
import org.springframework.stereotype.Service;

@Service
public class AuthService  {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RegisterUserProfileUseCase registerUserProfileUseCase;
    private final LoadUserProfilePort loadUserProfilePort;

    @Autowired
    public AuthService(@Lazy AuthenticationManager authenticationManager, JwtService jwtService, RegisterUserProfileUseCase registerUserProfileUseCase, LoadUserProfilePort loadUserProfilePort){
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.registerUserProfileUseCase = registerUserProfileUseCase;
        this.loadUserProfilePort = loadUserProfilePort;
    }
    public AuthenticationResponse register(AuthenticationRequest request) {
        UserProfile registered = registerUserProfileUseCase.register(new RegisterUserProfileCommand(
                request.getUsername(), request.getFullName(), request.getEmail(), request.getPassword()));

        String jwt = jwtService.generateToken(new SecurityUserDetails(registered));
        return new AuthenticationResponse(jwt, "Bearer", jwtService.getExpirationMs() / 1000);
    }

    public AuthenticationResponse login(LoginRequest request) {
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
                request.getUsername(), request.getPassword()));

        UserProfile user = loadUserProfilePort.findByUsername(request.getUsername())
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

        String jwt = jwtService.generateToken(new SecurityUserDetails(user));
        return new AuthenticationResponse(jwt, "Bearer", jwtService.getExpirationMs() / 1000);
    }
}
