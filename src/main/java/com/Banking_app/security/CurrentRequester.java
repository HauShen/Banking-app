package com.Banking_app.security;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Injects the logged-in user as a {@link com.Banking_app.shared.Requester} (user id + whether they are an admin)
 * into a controller method:
 *
 * <pre>{@code
 * @GetMapping("/{id}")
 * public AccountResponseBody get(@PathVariable Long id, @CurrentRequester Requester requester) { ... }
 * }</pre>
 *
 * Resolved by {@link CurrentRequesterArgumentResolver}; this is the only place that turns Spring Security's
 * {@code Authentication} into the plain record the use cases understand. Hidden from Swagger (see OpenApiConfig).
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CurrentRequester {
}
