package com.Banking_app.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Replaces the generated {@code ApplicationTests}. Loading the context against a real
 * PostgreSQL container also proves that every Flyway migration applies cleanly and that
 * the JPA entity mappings still validate against the resulting schema
 * ({@code spring.jpa.hibernate.ddl-auto=validate}).
 */
@DisplayName("Application context")
class ApplicationContextIntegrationTest extends AbstractIntegrationTest {

    @Test
    @DisplayName("starts, applies all migrations, and validates the entity mappings")
    void contextLoads() {
        // The assertion is the successful context startup performed by the base class.
    }
}
