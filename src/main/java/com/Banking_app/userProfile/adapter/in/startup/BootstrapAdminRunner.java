package com.Banking_app.userProfile.adapter.in.startup;
import com.Banking_app.userProfile.application.port.in.command.RegisterUserProfileCommand;
import com.Banking_app.userProfile.application.port.in.usecase.BootstrapFirstAdminUseCase;
import com.Banking_app.userProfile.domain.models.userprofile.UserProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class BootstrapAdminRunner implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(BootstrapAdminRunner.class);

    private final BootstrapFirstAdminUseCase bootstrapFirstAdminUseCase;
    private final BootstrapAdminProperties properties;

    public BootstrapAdminRunner(BootstrapFirstAdminUseCase bootstrapFirstAdminUseCase,
                                BootstrapAdminProperties properties) {
        this.bootstrapFirstAdminUseCase = bootstrapFirstAdminUseCase;
        this.properties = properties;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!properties.isEnabled()) {
            return;
        }
        if (properties.getPassword() == null || properties.getPassword().isBlank()) {
            // Fail loudly rather than inventing a password or silently skipping.
            throw new IllegalStateException(
                    "banking.bootstrap.admin.enabled=true but no password was supplied. "
                            + "Set BANKING_BOOTSTRAP_ADMIN_PASSWORD in the environment.");
        }

        Optional<UserProfile> created = bootstrapFirstAdminUseCase.bootstrapIfNoAdminExists(
                new RegisterUserProfileCommand(
                        properties.getUsername(),
                        properties.getFullName(),
                        properties.getEmail(),
                        properties.getPassword()));

        created.ifPresentOrElse(
                admin -> log.warn("Bootstrap admin '{}' created. This account is temporary: "
                                + "sign in, create a personal admin account, then delete it.",
                        admin.getUsername()),
                () -> log.info("Bootstrap skipped: an administrator already exists."));
    }
}
