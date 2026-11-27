package com.Banking_app.demo.adapter.in.web;

import com.Banking_app.demo.DemoConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/** The demo guard exists only with the demo profile: outside the demo, seeded-looking data is ordinary data. */
class DemoGuardConfigTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(DemoConfig.class, DemoGuardConfig.class)
            .withBean(JdbcTemplate.class, () -> mock(JdbcTemplate.class))
            .withPropertyValues("app.demo.customer-password=Demo1234!", "app.demo.admin-password=DemoAdmin1!");

    @Test
    void withoutDemoProfile_noGuard() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).doesNotHaveBean(DemoGuardConfig.class);
            assertThat(context).doesNotHaveBean(DemoGuardInterceptor.class);
        });
    }

    @Test
    void withDemoProfile_theGuardIsThere() {
        runner.withPropertyValues("spring.profiles.active=demo").run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(DemoGuardInterceptor.class);
        });
    }
}
