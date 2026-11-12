package com.Banking_app.security;

import jakarta.servlet.DispatcherType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.context.annotation.Lazy;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig{
    private final JwtRequestFilter jwtRequestFilter;
    private final JsonSecurityErrorHandler securityErrorHandler;
    private final List<String> allowedOrigins;

    @Autowired
    public SecurityConfig(@Lazy JwtRequestFilter jwtRequestFilter,
                          JsonSecurityErrorHandler securityErrorHandler,
                          @Value("${app.cors.allowed-origins:http://localhost:5173}") String[] allowedOrigins){
        this.jwtRequestFilter = jwtRequestFilter;
        this.securityErrorHandler = securityErrorHandler;
        this.allowedOrigins = Arrays.stream(allowedOrigins).map(String::trim).filter(o -> !o.isEmpty()).toList();
    }
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Spring's internal forward to /error (after sendError, e.g. the 403 for a URL rule
                        // below). The original request was already authorized; without this the error
                        // request runs unauthenticated and turns every such 403 into a 401.
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/logout").authenticated()   // before the permitAll below
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        // Health check for Docker and deploy scripts: answers UP or DOWN only, no details
                        // (management.endpoint.health.show-details=never). Nothing else under /actuator is public.
                        .requestMatchers(HttpMethod.GET, "/actuator/health").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")   // URL-level, defence in depth
                        .anyRequest().authenticated())
                // Not logged in (no token, or a token the filter rejected) -> 401.
                // Logged in but not allowed (e.g. a customer on /api/admin/**) -> 403.
                // Both answer with the same JSON ApiError body as GlobalExceptionHandler, written directly
                // (no sendError), so they don't depend on the internal forward to /error.
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(securityErrorHandler)
                        .accessDeniedHandler(securityErrorHandler))

                .addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }


    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        // From app.cors.allowed-origins (env CORS_ALLOWED_ORIGINS), so a deployed frontend needs no code change.
        config.setAllowedOrigins(allowedOrigins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        // Only the headers the frontend actually sends (Accept etc. are always allowed by the CORS spec).
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }




    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

}
