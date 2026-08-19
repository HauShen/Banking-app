package com.Banking_app.security;
import com.Banking_app.userProfile.adapter.out.persistence.entities.UserProfileJpaEntity;
import com.Banking_app.userProfile.domain.enums.UserRole;
import com.Banking_app.userProfile.adapter.out.persistence.jparepositories.UserProfileJpaRepository;
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
    private final UserProfileJpaRepository userProfileJpaRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    @Autowired
    public AuthService(UserProfileJpaRepository userProfileJpaRepository, PasswordEncoder passwordEncoder, @Lazy AuthenticationManager authenticationManager, JwtService jwtService){
        this.userProfileJpaRepository = userProfileJpaRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }
    public AuthenticationResponse register(AuthenticationRequest request) {
        if (userProfileJpaRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username already exists");
        }
        if (userProfileJpaRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }

        Instant now = Instant.now();

        UserProfileJpaEntity user = new UserProfileJpaEntity();
        user.setId(UUID.randomUUID().toString());
        user.setUsername(request.getUsername());
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword())); // critical
        // Public self-registration always produces a CUSTOMER. The role must never be taken
        // from the request body: /api/auth/register is unauthenticated, so honouring a
        // client-supplied role would let anyone mint themselves an admin account.
        // Elevating a user is an admin-only operation (PATCH /api/users/admin/{id}/role).
        user.setRole(UserRole.CUSTOMER);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);

        UserProfileJpaEntity saved = userProfileJpaRepository.save(user);

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

        UserDetails user = userProfileJpaRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

        String jwt = jwtService.generateToken(user);
        return new AuthenticationResponse(jwt, "Bearer", jwtService.getExpirationMs() / 1000);
    }
}
