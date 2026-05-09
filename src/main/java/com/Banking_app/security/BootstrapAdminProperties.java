package com.Banking_app.security;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "bootstrap.admin")
@Getter
@Setter
public class BootstrapAdminProperties {
    /**
     * Enable/disable bootstrap creation logic.
     */
    private boolean enabled = true;
    /**
     * Bootstrap admin username (prefer env var in real deployments).
     */
    private String username;

    /**
     * Bootstrap admin email (prefer env var in real deployments).
     */
    private String email;

    /**
     * Bootstrap admin raw password (never log this).
     */
    private String password;

    public boolean isEnabled() {
        return enabled;
    }

}
