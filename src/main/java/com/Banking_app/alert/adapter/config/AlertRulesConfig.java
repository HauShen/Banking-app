package com.Banking_app.alert.adapter.config;

import com.Banking_app.alert.domain.models.AlertRules;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Builds the domain's AlertRules from configuration, so the domain stays plain Java. */
@Configuration
@EnableConfigurationProperties(AlertProperties.class)
public class AlertRulesConfig {

    @Bean
    public AlertRules alertRules(AlertProperties properties) {
        return new AlertRules(properties.largeAmount(), properties.rapidMaxTransfers(), properties.rapidWindow());
    }
}
