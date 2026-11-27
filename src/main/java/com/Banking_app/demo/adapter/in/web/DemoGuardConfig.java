package com.Banking_app.demo.adapter.in.web;

import com.Banking_app.demo.DemoCast;
import com.Banking_app.demo.DemoProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Registers the {@link DemoGuardInterceptor} for {@code /api/**}, only with the {@code demo} profile. */
@Configuration
@Profile("demo")
public class DemoGuardConfig implements WebMvcConfigurer {

    private final JdbcTemplate jdbc;
    private final DemoProperties properties;

    public DemoGuardConfig(JdbcTemplate jdbc, DemoProperties properties) {
        this.jdbc = jdbc;
        this.properties = properties;
    }

    @Bean
    public DemoGuardInterceptor demoGuardInterceptor() {
        return new DemoGuardInterceptor(jdbc, DemoCast.standard(properties.historyDays()));
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(demoGuardInterceptor()).addPathPatterns("/api/**");
    }
}
