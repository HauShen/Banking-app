package com.Banking_app.notification.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Reads "Authorization: Bearer &lt;token&gt;" and, if {@link AccessTokenVerifier} accepts the token, marks the request
 * as logged in. A missing or bad token leaves the request anonymous: SecurityConfig then answers 401.
 *
 * <p>Not a @Component on purpose: Spring Boot would also register a Filter bean in the servlet container, outside
 * the security chain. SecurityConfig creates it and adds it to the chain only.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final AccessTokenVerifier verifier;

    public JwtAuthenticationFilter(AccessTokenVerifier verifier) {
        this.verifier = verifier;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            try {
                SecurityContextHolder.getContext().setAuthentication(verifier.verify(header.substring(7)));
            } catch (JwtException | IllegalArgumentException e) {
                log.debug("Rejected JWT on {}: {}", request.getRequestURI(), e.getMessage());
                SecurityContextHolder.clearContext();
            }
        }
        // Outside the try on purpose: exceptions from controllers further down must not be swallowed here.
        chain.doFilter(request, response);
    }
}
