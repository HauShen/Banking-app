package com.Banking_app.notification.security;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final CurrentRequesterArgumentResolver currentRequesterArgumentResolver;

    public WebMvcConfig(CurrentRequesterArgumentResolver currentRequesterArgumentResolver) {
        this.currentRequesterArgumentResolver = currentRequesterArgumentResolver;
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(currentRequesterArgumentResolver);
    }
}
