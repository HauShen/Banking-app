package com.Banking_app.security;
import com.Banking_app.models.UserProfile;
import com.Banking_app.models.enums.UserRole;
import com.Banking_app.repositories.UserProfileRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;

@Slf4j
@Component
public class BootstrapAdminRunner implements CommandLineRunner{
    private final UserProfileRepository userProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final Environment env;
    private final BootstrapAdminProperties props;
    @Autowired
    public BootstrapAdminRunner(UserProfileRepository userProfileRepository, PasswordEncoder passwordEncoder,Environment env, BootstrapAdminProperties props){
        this.userProfileRepository = userProfileRepository;
        this.passwordEncoder = passwordEncoder;
        this.env = env;
        this.props = props;
    }
    @Override
    public void run(String... args) {
        if (!props.isEnabled()) {
            log.info("Bootstrap admin skipped: bootstrap.admin.enabled=false");
            return;
        }

        // 1) If admin already exists, skip
        if (userProfileRepository.countByRole(UserRole.ADMIN) > 0) {
            log.info("Bootstrap admin skipped: ADMIN already exists.");
            return;
        }
        // 2) Read env vars
        String username = env.getProperty("BOOTSTRAP_ADMIN_USERNAME");
        String email = env.getProperty("BOOTSTRAP_ADMIN_EMAIL");
        String rawPassword = env.getProperty("BOOTSTRAP_ADMIN_PASSWORD");

        if (!StringUtils.hasText(username) || !StringUtils.hasText(email) || !StringUtils.hasText(rawPassword)) {
            log.warn("Bootstrap admin skipped: missing BOOTSTRAP_ADMIN_USERNAME / BOOTSTRAP_ADMIN_EMAIL / BOOTSTRAP_ADMIN_PASSWORD.");
            return;
        }
        // 3) Prevent duplicate by email
        if (userProfileRepository.existsByEmail(email)) {
            log.warn("Bootstrap admin skipped: user email already exists: {}", email);
            return;
        }
        // 4) Prevent duplicate by username
        if (userProfileRepository.existsByEmail(email)) {
            log.warn("Bootstrap admin skipped: user email already exists: {}", email);
            return;
        }

        // 5) Create ADMIN
        UserProfile admin = new UserProfile();
        admin.setUsername(username);
        admin.setEmail(email.trim().toLowerCase());
        admin.setPassword(passwordEncoder.encode(rawPassword));
        admin.setRole(UserRole.ADMIN);
        admin.setCreatedAt(Instant.now());

        userProfileRepository.save(admin);
        log.info("Bootstrap admin created successfully for Username={}", admin.getUsername());

    }
}

