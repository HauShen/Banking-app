package com.Banking_app.config;

import com.Banking_app.security.CurrentRequesterArgumentResolver;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/** Spring MVC additions. CORS stays in SecurityConfig. */
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
