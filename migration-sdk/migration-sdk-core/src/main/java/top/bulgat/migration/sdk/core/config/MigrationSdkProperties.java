package top.bulgat.migration.sdk.core.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * SDK 运行时配置。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MigrationSdkProperties {

    /** 配置中心地址环境变量。 */
    public static final String ENV_CONFIG_CENTER_URL = "MIGRATION_CONFIG_CENTER_URL";
    /** Diff 服务地址环境变量。 */
    public static final String ENV_DIFF_SERVICE_URL = "MIGRATION_DIFF_SERVICE_URL";
    /** 默认超时环境变量。 */
    public static final String ENV_DEFAULT_TIMEOUT = "MIGRATION_DEFAULT_TIMEOUT";
    /** 内部令牌环境变量。 */
    public static final String ENV_INTERNAL_TOKEN = "MIGRATION_INTERNAL_TOKEN";
    /** 缓存定时刷新间隔（秒）环境变量。 */
    public static final String ENV_CACHE_REFRESH_INTERVAL = "MIGRATION_CACHE_REFRESH_INTERVAL";
    /** 百分比路由策略环境变量。 */
    public static final String ENV_PERCENTAGE_ROUTING_STRATEGY = "MIGRATION_PERCENTAGE_ROUTING_STRATEGY";

    private String configCenterUrl;
    private String diffServiceUrl;
    private int defaultTimeout;
    private String internalToken;
    private int cacheRefreshIntervalSeconds;
    private String percentageRoutingStrategy;

    /**
     * 从环境变量构建配置。
     *
     * @return SDK 配置
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

        int cacheInterval = 60;
        String cacheIntervalEnv = System.getenv(ENV_CACHE_REFRESH_INTERVAL);
        if (cacheIntervalEnv != null) {
            try {
                cacheInterval = Integer.parseInt(cacheIntervalEnv);
            } catch (NumberFormatException ignored) {
                cacheInterval = 60;
            }
        }

        return MigrationSdkProperties.builder()
                .configCenterUrl(resolve(ENV_CONFIG_CENTER_URL, "http://localhost:8080"))
                .diffServiceUrl(resolve(ENV_DIFF_SERVICE_URL, "http://localhost:8081"))
                .internalToken(resolve(ENV_INTERNAL_TOKEN, "MIGRATION_DEFAULT_SDK_TOKEN"))
                .defaultTimeout(timeout)
                .cacheRefreshIntervalSeconds(cacheInterval)
                .percentageRoutingStrategy(resolve(ENV_PERCENTAGE_ROUTING_STRATEGY,
                        top.bulgat.migration.sdk.core.model.PercentageRoutingStrategy.HASH))
                .build();
    }

    /**
     * 解析环境变量，缺失时回退默认值。
     *
     * @param key          env key
     * @param defaultValue 默认值
     * @return 解析结果
     */
    private static String resolve(String key, String defaultValue) {
        String value = System.getenv(key);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return value;
    }
}
