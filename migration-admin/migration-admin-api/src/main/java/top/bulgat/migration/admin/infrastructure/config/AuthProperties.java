package top.bulgat.migration.admin.infrastructure.config;

import java.nio.charset.StandardCharsets;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import jakarta.annotation.PostConstruct;

@Data
@Configuration
@ConfigurationProperties(prefix = "migration-admin.auth")
public class AuthProperties {

    private static final int MIN_JWT_SECRET_BYTES = 32;

    private String username;
    private String password;
    private String displayName = "System Administrator";
    private String jwtSecret;
    private long jwtExpirationMs = 86400000; // 默认 24 小时
    private String internalToken;

    @PostConstruct
    public void validate() {
        requireText(username, "migration-admin.auth.username");
        requireText(password, "migration-admin.auth.password");
        requireText(jwtSecret, "migration-admin.auth.jwt-secret");
        requireText(internalToken, "migration-admin.auth.internal-token");
        int secretBytes = jwtSecret.getBytes(StandardCharsets.UTF_8).length;
        if (secretBytes < MIN_JWT_SECRET_BYTES) {
            throw new IllegalStateException("migration-admin.auth.jwt-secret must be at least 32 bytes for HS256");
        }
    }

    private void requireText(String value, String field) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalStateException(field + " must be configured");
        }
    }
}
