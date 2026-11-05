package com.Banking_app.security;

import com.Banking_app.shared.Requester;
import com.Banking_app.userProfile.adapter.out.security.SecurityUserDetails;
import org.springframework.core.MethodParameter;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * Builds the {@link Requester} for parameters annotated with {@link CurrentRequester}.
 * Replaces the private {@code requester(currentUserId, authentication)} helpers that AccountController and
 * TransferController each had. Registered in {@code config.WebMvcConfig}.
 */
@Component
public class CurrentRequesterArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentRequester.class)
                && Requester.class.equals(parameter.getParameterType());
    }

    @Override
    public Requester resolveArgument(MethodParameter parameter,
                                     ModelAndViewContainer mavContainer,
                                     NativeWebRequest webRequest,
                                     WebDataBinderFactory binderFactory) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // SecurityConfig already rejects unauthenticated calls to these endpoints with 401, so this only
        // happens if an endpoint is made public by mistake. Fail as 401 (AuthenticationException handler)
        // instead of a NullPointerException → 500.
        if (authentication == null || !(authentication.getPrincipal() instanceof SecurityUserDetails user)) {
            throw new AuthenticationCredentialsNotFoundException("Authentication required");
        }

        boolean admin = authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
        return new Requester(user.getUserId(), admin);
    }
}
