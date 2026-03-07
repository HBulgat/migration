package top.bulgat.migration.admin.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import jakarta.annotation.PostConstruct;

@Configuration
@ConfigurationProperties(prefix = "migration.admin")
public class InternalTokenProperties {

    private String internalToken;

    @PostConstruct
    public void validate() {
        if (!StringUtils.hasText(internalToken)) {
            throw new IllegalStateException("migration.admin.internal-token must be configured");
        }
    }

    public String getInternalToken() {
        return internalToken;
    }

    public void setInternalToken(String internalToken) {
        this.internalToken = internalToken;
    }
}
