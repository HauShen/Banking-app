package com.Banking_app.demo;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Switches the live-demo parts on, only with the {@code demo} profile. Without the profile none of this exists:
 * no demo settings are bound, and (in later steps) no seeder and no reset job.
 *
 * <p>The demo creates users with public passwords, so it says so loudly at startup. A real deployment that shows
 * this warning has the wrong profile.
 */
@Configuration
@Profile("demo")
@EnableConfigurationProperties(DemoProperties.class)
public class DemoConfig {

    private static final Logger log = LoggerFactory.getLogger(DemoConfig.class);

    private final DemoProperties properties;

    public DemoConfig(DemoProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    void warnThatThisIsTheDemo() {
        log.warn("DEMO PROFILE ACTIVE: demo users with public passwords are created and data is reset. "
                + "Never use this profile for a real deployment. ({} days of history, seed {})",
                properties.historyDays(), properties.randomSeed());
    }
}
