package com.Banking_app.notification.demo;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * The live demo's nightly reset of this service, only with the {@code demo} profile. Scheduling is switched on
 * here, and only here: outside the demo the service has nothing to schedule, and stays exactly as it was.
 */
@Configuration
@Profile("demo")
@EnableScheduling
public class DemoNotificationsResetConfig {

    @Bean
    public DemoNotificationsReset demoNotificationsReset(JdbcTemplate jdbc,
                                                         PlatformTransactionManager transactionManager) {
        return new DemoNotificationsReset(jdbc, transactionManager);
    }
}
