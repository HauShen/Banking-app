package com.Banking_app.notification.demo;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/** The reset exists only with the demo profile; outside the demo the service is unchanged. No database needed. */
class DemoNotificationsResetConfigTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(DemoNotificationsResetConfig.class)
            .withBean(JdbcTemplate.class, () -> mock(JdbcTemplate.class))
            .withBean(PlatformTransactionManager.class, () -> mock(PlatformTransactionManager.class));

    @Test
    void withoutDemoProfile_noReset() {
        runner.run(context -> assertThat(context).doesNotHaveBean(DemoNotificationsReset.class));
    }

    @Test
    void withDemoProfile_theResetExists() {
        runner.withPropertyValues("spring.profiles.active=demo")
                .run(context -> assertThat(context).hasSingleBean(DemoNotificationsReset.class));
    }
}
