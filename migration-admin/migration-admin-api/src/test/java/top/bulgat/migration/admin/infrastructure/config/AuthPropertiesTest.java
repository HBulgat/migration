package top.bulgat.migration.admin.infrastructure.config;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class AuthPropertiesTest {

    @Test
    void validate_shouldRejectMissingCredentials() {
        AuthProperties properties = new AuthProperties();
        properties.setDisplayName("Admin");

        IllegalStateException ex = assertThrows(IllegalStateException.class, properties::validate);

        org.junit.jupiter.api.Assertions.assertEquals("migration.auth.username must be configured", ex.getMessage());
    }

    @Test
    void validate_shouldRejectShortJwtSecret() {
        AuthProperties properties = new AuthProperties();
        properties.setUsername("admin");
        properties.setPassword("password");
        properties.setJwtSecret("short-secret");

        IllegalStateException ex = assertThrows(IllegalStateException.class, properties::validate);

        org.junit.jupiter.api.Assertions.assertEquals(
                "migration.auth.jwt-secret must be at least 32 bytes for HS256",
                ex.getMessage());
    }

    @Test
    void validate_shouldAcceptConfiguredValues() {
        AuthProperties properties = new AuthProperties();
        properties.setUsername("admin");
        properties.setPassword("password");
        properties.setJwtSecret("12345678901234567890123456789012");

        assertDoesNotThrow(properties::validate);
    }
}
