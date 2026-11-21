package com.Banking_app.alert.adapter.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.math.BigDecimal;
import java.time.Duration;

/**
 * Limits of the watch rules ({@code app.alerts.*}).
 *
 * @param largeAmount       a transfer of at least this much MYR raises a LARGE_TRANSFER alert
 * @param rapidMaxTransfers more than this many transfers out of one account within {@code rapidWindow} raise a
 *                          RAPID_TRANSFERS alert
 */
@ConfigurationProperties(prefix = "app.alerts")
public record AlertProperties(
        @DefaultValue("10000.00") BigDecimal largeAmount,
        @DefaultValue("5") int rapidMaxTransfers,
        @DefaultValue("10m") Duration rapidWindow) {
}
