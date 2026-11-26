package com.Banking_app.demo;

import com.Banking_app.transaction.application.port.in.usecase.RecordForeignRemittanceUseCase;
import com.Banking_app.transaction.application.port.in.usecase.TransferMoneyUseCase;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;

/**
 * The seeding and reset beans of the live demo (plan steps 5, 5b and 7), only with the {@code demo} profile.
 *
 * <p>Kept apart from {@link DemoConfig} because these beans need the database (JdbcTemplate, the transaction
 * manager) and the security setup (PasswordEncoder), while DemoConfig only binds the settings and can be tested
 * without any of that.
 *
 * <p>The story uses the same time zone and exchange rates as the rest of the app: {@code bank.time-zone} and the
 * bank's buy rates from {@code fx.rates.<CCY>.buy}. They are read as plain properties, so the demo package doesn't
 * depend on the fx module's classes.
 */
@Configuration
@Profile("demo")
public class DemoSeedConfig {

    @Bean
    public DemoStoryGenerator demoStoryGenerator(@Value("${bank.time-zone:Asia/Kuala_Lumpur}") ZoneId zone,
                                                 Environment environment) {
        Map<String, BigDecimal> buyRates = new HashMap<>();
        for (String currency : DemoStoryGenerator.requiredCurrencies()) {
            String rate = environment.getProperty("fx.rates." + currency + ".buy");
            if (rate != null) {
                buyRates.put(currency, new BigDecimal(rate));
            }
        }
        return new DemoStoryGenerator(zone, buyRates);   // rejects a missing rate: fails at startup, not at 4 am
    }

    @Bean
    public DemoDataWriter demoDataWriter(JdbcTemplate jdbc, PlatformTransactionManager transactionManager,
                                         PasswordEncoder passwordEncoder, DemoProperties properties,
                                         @Value("${bank.time-zone:Asia/Kuala_Lumpur}") ZoneId zone) {
        return new DemoDataWriter(jdbc, transactionManager, passwordEncoder, properties, zone);
    }

    @Bean
    public DemoWarmUp demoWarmUp(TransferMoneyUseCase transfers, RecordForeignRemittanceUseCase remittances) {
        return new DemoWarmUp(transfers, remittances);
    }

    @Bean
    public DemoResetJob demoResetJob(JdbcTemplate jdbc, PlatformTransactionManager transactionManager,
                                     DemoDataSeeder seeder) {
        return new DemoResetJob(jdbc, transactionManager, seeder);
    }

    @Bean
    public DemoDataSeeder demoDataSeeder(JdbcTemplate jdbc, DemoStoryGenerator generator, DemoDataWriter writer,
                                         DemoWarmUp warmUp, DemoProperties properties, Clock clock) {
        return new DemoDataSeeder(jdbc, generator, writer, warmUp, properties, clock);
    }
}
