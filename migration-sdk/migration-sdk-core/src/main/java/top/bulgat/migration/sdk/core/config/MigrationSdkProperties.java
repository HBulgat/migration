package top.bulgat.migration.sdk.core.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * SDK runtime properties.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MigrationSdkProperties {

    /** Environment variable for config center url. */
    public static final String ENV_CONFIG_CENTER_URL = "MIGRATION_CONFIG_CENTER_URL";
    /** Environment variable for diff service url. */
    public static final String ENV_DIFF_SERVICE_URL = "MIGRATION_DIFF_SERVICE_URL";
    /** Environment variable for default timeout. */
    public static final String ENV_DEFAULT_TIMEOUT = "MIGRATION_DEFAULT_TIMEOUT";
    /** Environment variable for internal token. */
    public static final String ENV_INTERNAL_TOKEN = "MIGRATION_INTERNAL_TOKEN";

    private String configCenterUrl;
    private String diffServiceUrl;
    private int defaultTimeout;
    private String internalToken;

    /**
     * Builds properties from environment variables.
     *
     * @return sdk properties
     */
    public static MigrationSdkProperties fromEnv() {
        int timeout = 5000;
        String timeoutEnv = System.getenv(ENV_DEFAULT_TIMEOUT);
        if (timeoutEnv != null) {
            try {
                timeout = Integer.parseInt(timeoutEnv);
            } catch (NumberFormatException ignored) {
                timeout = 5000;
            }
        }
        return MigrationSdkProperties.builder()
                .configCenterUrl(resolve(ENV_CONFIG_CENTER_URL, "http://localhost:8080"))
                .diffServiceUrl(resolve(ENV_DIFF_SERVICE_URL, "http://localhost:8081"))
                .internalToken(resolve(ENV_INTERNAL_TOKEN, "MIGRATION_DEFAULT_SDK_TOKEN"))
                .defaultTimeout(timeout)
                .build();
    }

    /**
     * Resolves environment variable with default fallback.
     *
     * @param key          env key
     * @param defaultValue fallback value
     * @return resolved value
     */
    private static String resolve(String key, String defaultValue) {
        String value = System.getenv(key);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return value;
    }
}
