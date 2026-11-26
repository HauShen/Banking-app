package com.Banking_app.demo;

import org.junit.jupiter.api.Test;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.Clock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * The seeding beans exist only with the demo profile and get their settings. The database beans are mocks: a
 * context runner never runs ApplicationRunners, so the seeder is created but never touches the database.
 */
class DemoSeedConfigTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(DemoConfig.class, DemoSeedConfig.class)
            .withBean(JdbcTemplate.class, () -> mock(JdbcTemplate.class))
            .withBean(PlatformTransactionManager.class, () -> mock(PlatformTransactionManager.class))
            .withBean(PasswordEncoder.class, () -> mock(PasswordEncoder.class))
            .withBean(Clock.class, Clock::systemUTC)
            .withPropertyValues("app.demo.customer-password=Demo1234!", "app.demo.admin-password=DemoAdmin1!",
                    "fx.rates.USD.buy=4.1650", "fx.rates.SGD.buy=3.2100");

    @Test
    void withoutDemoProfile_noSeedingBeans() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).doesNotHaveBean(DemoStoryGenerator.class);
            assertThat(context).doesNotHaveBean(DemoDataWriter.class);
            assertThat(context).doesNotHaveBean(DemoDataSeeder.class);
        });
    }

    @Test
    void withDemoProfile_theSeederIsAnApplicationRunner() {
        runner.withPropertyValues("spring.profiles.active=demo").run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(DemoStoryGenerator.class);
            assertThat(context).hasSingleBean(DemoDataWriter.class);
            assertThat(context).hasSingleBean(DemoDataSeeder.class);
            assertThat(context.getBeansOfType(ApplicationRunner.class).values())
                    .containsExactly(context.getBean(DemoDataSeeder.class));
        });
    }

    @Test
    void withDemoProfile_aMissingExchangeRate_failsAtStartup() {
        new ApplicationContextRunner()
                .withUserConfiguration(DemoConfig.class, DemoSeedConfig.class)
                .withBean(JdbcTemplate.class, () -> mock(JdbcTemplate.class))
                .withBean(PlatformTransactionManager.class, () -> mock(PlatformTransactionManager.class))
                .withBean(PasswordEncoder.class, () -> mock(PasswordEncoder.class))
                .withBean(Clock.class, Clock::systemUTC)
                .withPropertyValues("spring.profiles.active=demo",
                        "app.demo.customer-password=Demo1234!", "app.demo.admin-password=DemoAdmin1!",
                        "fx.rates.USD.buy=4.1650")   // no SGD rate
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void requiredCurrencies_areTheOnesTheRemittancesUse() {
        assertThat(DemoStoryGenerator.requiredCurrencies()).containsExactlyInAnyOrder("SGD", "USD");
    }
}
