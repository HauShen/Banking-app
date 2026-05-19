package com.Banking_app.security;
import com.Banking_app.models.UserProfile;
import com.Banking_app.models.enums.UserRole;
import com.Banking_app.repositories.UserProfileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.*;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.UUID;

@Service
public class AuthService {
    private final UserProfileRepository userProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    @Autowired
    public AuthService(UserProfileRepository userProfileRepository, PasswordEncoder passwordEncoder, @Lazy AuthenticationManager authenticationManager, JwtService jwtService){
        this.userProfileRepository = userProfileRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }
    public AuthenticationResponse register(AuthenticationRequest request) {
        if (userProfileRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username already exists");
        }
        if (userProfileRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }

        Instant now = Instant.now();

        UserProfile user = new UserProfile();
        user.setId(UUID.randomUUID().toString());
        user.setUsername(request.getUsername());
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword())); // critical
        user.setRole(request.getRole() == null ? UserRole.CUSTOMER : request.getRole());
        user.setCreatedAt(now);
        user.setUpdatedAt(now);

        UserProfile saved = userProfileRepository.save(user);

        String jwt = jwtService.generateToken(saved);
        return new AuthenticationResponse(jwt, "Bearer", jwtService.getExpirationMs() / 1000);
    }

    public AuthenticationResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()
                )
        );

        UserDetails user = userProfileRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

        String jwt = jwtService.generateToken(user);
        return new AuthenticationResponse(jwt, "Bearer", jwtService.getExpirationMs() / 1000);
    }
}
