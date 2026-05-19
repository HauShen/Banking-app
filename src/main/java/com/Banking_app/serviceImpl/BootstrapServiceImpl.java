package com.Banking_app.serviceImpl;

import com.Banking_app.dto.responseBodies.BootstrapStatusResponse;
import com.Banking_app.models.UserProfile;
import com.Banking_app.models.enums.UserRole;
import com.Banking_app.repositories.UserProfileRepository;
import com.Banking_app.service.BootstrapService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class BootstrapServiceImpl implements BootstrapService {
    private final UserProfileRepository userProfileRepository;
    private final PasswordEncoder passwordEncoder;
    @Autowired
    public BootstrapServiceImpl(UserProfileRepository userProfileRepository, PasswordEncoder passwordEncoder){
        this.userProfileRepository = userProfileRepository;
        this.passwordEncoder = passwordEncoder;
    }
    @Value("${bootstrap.admin.enabled:true}")
    private boolean bootstrapEnabled;
    public BootstrapStatusResponse getStatus() {
        boolean adminExists = userProfileRepository.existsByRole(UserRole.ADMIN);
        long userCount = userProfileRepository.count();
        boolean allowed = bootstrapEnabled && !adminExists; // optionally && userCount == 0
        return new BootstrapStatusResponse(bootstrapEnabled, adminExists, userCount, allowed);
    }
    @Override
    @Transactional
    public synchronized UserProfile registerFirstAdmin(String username, String fullName, String email, String rawPassword) {
        if (!bootstrapEnabled) {
            throw new IllegalStateException("Bootstrap admin registration is disabled");
        }
        if (userProfileRepository.existsByRole(UserRole.ADMIN)) {
            throw new IllegalStateException("Admin already exists");
        }
        if (userProfileRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Username already exists");
        }
        if (userProfileRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already exists");
        }

        Instant now = Instant.now();
        UserProfile user = new UserProfile();
        user.setId(UUID.randomUUID().toString());
        user.setUsername(username);
        user.setFullName(fullName);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setRole(UserRole.ADMIN);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);

        return userProfileRepository.save(user);
    }
}
