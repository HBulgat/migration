package top.bulgat.migration.admin.infrastructure.config;

import java.nio.charset.StandardCharsets;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import jakarta.annotation.PostConstruct;

@Configuration
@ConfigurationProperties(prefix = "migration.auth")
public class AuthProperties {

    private static final int MIN_JWT_SECRET_BYTES = 32;

    private String username;
    private String password;
    private String displayName = "System Administrator";
    private String jwtSecret;
    private long jwtExpirationMs = 86400000; // default 24 hours

    @PostConstruct
    public void validate() {
        requireText(username, "migration.auth.username");
        requireText(password, "migration.auth.password");
        requireText(jwtSecret, "migration.auth.jwt-secret");
        int secretBytes = jwtSecret.getBytes(StandardCharsets.UTF_8).length;
        if (secretBytes < MIN_JWT_SECRET_BYTES) {
            throw new IllegalStateException("migration.auth.jwt-secret must be at least 32 bytes for HS256");
        }
    }

    private void requireText(String value, String field) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalStateException(field + " must be configured");
        }
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getJwtSecret() {
        return jwtSecret;
    }

    public void setJwtSecret(String jwtSecret) {
        this.jwtSecret = jwtSecret;
    }

    public long getJwtExpirationMs() {
        return jwtExpirationMs;
    }

    public void setJwtExpirationMs(long jwtExpirationMs) {
        this.jwtExpirationMs = jwtExpirationMs;
    }
}
