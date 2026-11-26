package com.Banking_app.demo;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Settings of the public live demo ({@code app.demo.*}, set in application-demo.properties). Only bound while the
 * {@code demo} profile is active (see {@link DemoConfig}).
 *
 * <p>Validated at startup: a demo without passwords, or with a history the balance chart can't show, fails to start
 * instead of seeding broken data. The password rule is the same as for real users (8-100 characters).
 *
 * @param customerPassword password of every seeded customer; public on purpose
 * @param adminPassword    password of the seeded admin (demoadmin); public on purpose
 * @param randomSeed       seed of the demo data generator: the same seed gives the same data after every reset
 * @param historyDays      days of transaction history, counted back from today; at most the 366 days the balance
 *                         history can show
 */
@Validated
@ConfigurationProperties(prefix = "app.demo")
public record DemoProperties(
        @NotBlank @Size(min = 8, max = 100) String customerPassword,
        @NotBlank @Size(min = 8, max = 100) String adminPassword,
        @DefaultValue("42") long randomSeed,
        @DefaultValue("90") @Min(7) @Max(366) int historyDays) {

    /** Keeps the passwords out of logs and error messages that print this record. */
    @Override
    public String toString() {
        return "DemoProperties[customerPassword=***, adminPassword=***, randomSeed=" + randomSeed
                + ", historyDays=" + historyDays + "]";
    }
}
