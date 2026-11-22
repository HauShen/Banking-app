package com.Banking_app.notification.security;

import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Stateless: every request brings its access token. No login, no sessions, no CSRF token (nothing relies on cookies).
 * No CORS either: browsers reach this service through the frontend's nginx or the Vite dev server, on the same
 * origin as the app.
 */
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, AccessTokenVerifier verifier,
                                                   JsonSecurityErrorHandler errorHandler) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .httpBasic(basic -> basic.disable())
                .formLogin(form -> form.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Spring's internal forward to /error: the original request was already authorized.
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        // Health check for Docker: UP or DOWN only, no details.
                        .requestMatchers(HttpMethod.GET, "/actuator/health").permitAll()
                        .requestMatchers("/api/notifications/**").authenticated()
                        .anyRequest().denyAll())
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(errorHandler)
                        .accessDeniedHandler(errorHandler))
                .addFilterBefore(new JwtAuthenticationFilter(verifier), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /**
     * No users here. Without this bean Spring Boot would create a default user with a generated password (and log
     * it at startup).
     */
    @Bean
    public UserDetailsService noUsers() {
        return new InMemoryUserDetailsManager();
    }
}
