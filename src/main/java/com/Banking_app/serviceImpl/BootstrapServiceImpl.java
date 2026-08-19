package com.Banking_app.serviceImpl;

import com.Banking_app.dto.responseBodies.BootstrapStatusResponse;
import com.Banking_app.userProfile.adapter.out.persistence.entities.UserProfileJpaEntity;
import com.Banking_app.userProfile.domain.enums.UserRole;
import com.Banking_app.userProfile.adapter.out.persistence.jparepositories.UserProfileJpaRepository;
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
    private final UserProfileJpaRepository userProfileJpaRepository;
    private final PasswordEncoder passwordEncoder;
    @Autowired
    public BootstrapServiceImpl(UserProfileJpaRepository userProfileJpaRepository, PasswordEncoder passwordEncoder){
        this.userProfileJpaRepository = userProfileJpaRepository;
        this.passwordEncoder = passwordEncoder;
    }
    @Value("${bootstrap.admin.enabled:true}")
    private boolean bootstrapEnabled;
    public BootstrapStatusResponse getStatus() {
        boolean adminExists = userProfileJpaRepository.existsByRole(UserRole.ADMIN);
        long userCount = userProfileJpaRepository.count();
        boolean allowed = bootstrapEnabled && !adminExists; // optionally && userCount == 0
        return new BootstrapStatusResponse(bootstrapEnabled, adminExists, userCount, allowed);
    }
    @Override
    @Transactional
    public synchronized UserProfileJpaEntity registerFirstAdmin(String username, String fullName, String email, String rawPassword) {
        if (!bootstrapEnabled) {
            throw new IllegalStateException("Bootstrap admin registration is disabled");
        }
        if (userProfileJpaRepository.existsByRole(UserRole.ADMIN)) {
            throw new IllegalStateException("Admin already exists");
        }
        if (userProfileJpaRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Username already exists");
        }
        if (userProfileJpaRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already exists");
        }

        Instant now = Instant.now();
        UserProfileJpaEntity user = new UserProfileJpaEntity();
        user.setId(UUID.randomUUID().toString());
        user.setUsername(username);
        user.setFullName(fullName);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setRole(UserRole.ADMIN);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);

        return userProfileJpaRepository.save(user);
    }
}
