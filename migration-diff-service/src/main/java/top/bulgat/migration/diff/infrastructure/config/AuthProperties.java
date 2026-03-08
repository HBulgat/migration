package top.bulgat.migration.diff.infrastructure.config;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Configuration
@ConfigurationProperties(prefix = "migration-diff.auth")
@Data
public class AuthProperties {

    private String internalToken;

    @PostConstruct
    public void validate() {
        if (!StringUtils.hasText(internalToken)) {
            throw new IllegalStateException("migration.diff.internal-token must be configured");
        }
    }
}
