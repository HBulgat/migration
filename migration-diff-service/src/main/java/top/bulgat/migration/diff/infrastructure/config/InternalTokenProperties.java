package top.bulgat.migration.diff.infrastructure.config;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Configuration
@ConfigurationProperties(prefix = "migration.diff")
public class InternalTokenProperties {

    private String internalToken;

    @PostConstruct
    public void validate() {
        if (!StringUtils.hasText(internalToken)) {
            throw new IllegalStateException("migration.diff.internal-token must be configured");
        }
    }

    public String getInternalToken() {
        return internalToken;
    }

    public void setInternalToken(String internalToken) {
        this.internalToken = internalToken;
    }
}
