package com.Banking_app.demo;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The demo settings exist only with the demo profile, and application-demo.properties binds and validates.
 * Plain context runner: no database, no web server, no Docker needed.
 */
class DemoConfigTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(DemoConfig.class);

    @Test
    void withoutDemoProfile_nothingFromTheDemoExists() {
        runner.withPropertyValues("app.demo.customer-password=Demo1234!", "app.demo.admin-password=DemoAdmin1!")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean(DemoConfig.class);
                    assertThat(context).doesNotHaveBean(DemoProperties.class);
                });
    }

    @Test
    void withDemoProfile_theRealDemoPropertiesFileBinds() {
        // ConfigDataApplicationContextInitializer loads application.properties + application-demo.properties,
        // like a real start with SPRING_PROFILES_ACTIVE=demo.
        runner.withInitializer(new ConfigDataApplicationContextInitializer())
                .withPropertyValues("spring.profiles.active=demo")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    DemoProperties properties = context.getBean(DemoProperties.class);
                    assertThat(properties.customerPassword()).isEqualTo("Demo1234!");
                    assertThat(properties.adminPassword()).isEqualTo("DemoAdmin1!");
                    assertThat(properties.randomSeed()).isEqualTo(42L);
                    assertThat(properties.historyDays()).isEqualTo(90);
                    assertThat(context.getEnvironment().getProperty("springdoc.swagger-ui.enabled"))
                            .isEqualTo("true");
                });
    }

    @Test
    void withDemoProfile_defaultsApplyWhenOnlyPasswordsAreSet() {
        runner.withPropertyValues("spring.profiles.active=demo",
                        "app.demo.customer-password=Demo1234!", "app.demo.admin-password=DemoAdmin1!")
                .run(context -> {
                    DemoProperties properties = context.getBean(DemoProperties.class);
                    assertThat(properties.randomSeed()).isEqualTo(42L);
                    assertThat(properties.historyDays()).isEqualTo(90);
                });
    }

    @Test
    void withDemoProfile_missingPassword_failsAtStartup() {
        runner.withPropertyValues("spring.profiles.active=demo", "app.demo.admin-password=DemoAdmin1!")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void withDemoProfile_tooShortPassword_failsAtStartup() {
        runner.withPropertyValues("spring.profiles.active=demo",
                        "app.demo.customer-password=short", "app.demo.admin-password=DemoAdmin1!")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void withDemoProfile_historyLongerThanTheChartCanShow_failsAtStartup() {
        runner.withPropertyValues("spring.profiles.active=demo",
                        "app.demo.customer-password=Demo1234!", "app.demo.admin-password=DemoAdmin1!",
                        "app.demo.history-days=400")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void toString_hidesThePasswords() {
        DemoProperties properties = new DemoProperties("Demo1234!", "DemoAdmin1!", 42L, 90);
        assertThat(properties.toString()).doesNotContain("Demo1234!").doesNotContain("DemoAdmin1!");
    }
}
