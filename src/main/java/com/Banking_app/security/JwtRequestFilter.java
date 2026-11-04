package com.Banking_app.security;
import com.Banking_app.security.token.TokenVersionService;
import com.Banking_app.userProfile.adapter.out.security.SecurityUserDetails;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;


@Component
public class JwtRequestFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(JwtRequestFilter.class);
    private final CustomUserDetailsService customUserDetailsService;
    private final JwtService jwtService;
    private final TokenVersionService tokenVersionService;

    @Autowired
    public JwtRequestFilter(CustomUserDetailsService customUserDetailsService, JwtService jwtService,
                            TokenVersionService tokenVersionService){
        this.customUserDetailsService = customUserDetailsService;
        this.jwtService = jwtService;
        this.tokenVersionService = tokenVersionService;
    }
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)throws  ServletException, IOException {
        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String username;

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }
        jwt = authHeader.substring(7);

        // A bad token must not crash the request. Expired, tampered or garbage tokens (JwtException,
        // IllegalArgumentException) and valid tokens for a user who has since been deleted
        // (UsernameNotFoundException) all leave the request unauthenticated: protected endpoints then
        // answer 401 via the entry point in SecurityConfig, and public ones (e.g. /api/auth/login)
        // still work even if the client sends a stale token.
        try {
            username = jwtService.extractUsername(jwt);

            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                SecurityUserDetails userDetails = customUserDetailsService.loadUserByUsername(username);
                // A token issued before the user's last logout / revocation carries an old version -> rejected.
                int currentTokenVersion = tokenVersionService.currentVersion(userDetails.getUserId());

                if (jwtService.isTokenValid(jwt, userDetails, currentTokenVersion)) {
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    userDetails.getAuthorities()
                            );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (JwtException | IllegalArgumentException | UsernameNotFoundException e) {
            log.debug("Rejected JWT on {}: {}", request.getRequestURI(), e.getMessage());
            SecurityContextHolder.clearContext();
        }

        // Outside the try on purpose: exceptions from controllers further down must not be swallowed here.
        filterChain.doFilter(request, response);
    }

}
