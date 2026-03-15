package top.bulgat.migration.sdk.core.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import top.bulgat.common.base.util.StringUtils;

import java.util.Objects;
import java.util.Set;

/**
 * SDK 运行时配置。
 */
@Slf4j
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MigrationSdkProperties {

    // ----------config-center----------
    public static final String ENV_CONFIG_CENTER_ENABLE ="ENV_CONFIG_CENTER_ENABLE";
    /** 配置中心地址环境变量。 */
    public static final String ENV_CONFIG_CENTER_ADDRESS = "MIGRATION_CONFIG_CENTER_ADDRESS";
    /** 配置中心内部令牌环境变量。 */
    public static final String ENV_CONFIG_CENTER_INTERNAL_TOKEN="ENV_CONFIG_CENTER_INTERNAL_TOKEN";
    /** 是否启用缓存 */
    public static final String ENV_CONFIG_CENTER_CACHE_ENABLE="ENV_CONFIG_CENTER_CACHE_ENABLE";
    /** 缓存定时刷新间隔（秒）环境变量。 */
    public static final String ENV_CONFIG_CENTER_CACHE_REFRESH_INTERVAL_SECONDS = "ENV_CONFIG_CENTER_CACHE_REFRESH_INTERVAL_SECONDS";
    /** 超时时间 */
    public static final String ENV_CONFIG_CENTER_TIMEOUT = "ENV_CONFIG_CENTER_TIMEOUT";
    // ---------------------------------
    // ------------diff-service---------
    public static final String ENV_DIFF_SERVICE_ENABLE = "ENV_DIFF_SERVICE_ENABLE";
    /** Diff 服务地址环境变量。 */
    public static final String ENV_DIFF_SERVICE_ADDRESS = "ENV_DIFF_SERVICE_ADDRESS";
    /** Diff 服务内部令牌环境变量。 */
    public static final String ENV_DIFF_SERVICE_INTERNAL_TOKEN = "ENV_DIFF_SERVICE_INTERNAL_TOKEN";

    private Boolean configCenterEnable;
    private String configCenterAddress;
    private String configCenterInternalToken;
    private Boolean configCenterCacheEnable;
    private int configCenterCacheRefreshIntervalSeconds;
    private int configCenterTimeout;

    private Boolean diffServiceEnable=true;
    private String diffServiceAddress;
    private String diffServiceInternalToken;

    /**
     * 从环境变量构建配置。
     *
     * @return SDK 配置
     */
    public static MigrationSdkProperties fromEnv() {
        return MigrationSdkProperties.builder()
                .configCenterAddress(resolve(ENV_CONFIG_CENTER_ADDRESS, "http://localhost:8080"))
                .configCenterInternalToken(resolve(ENV_CONFIG_CENTER_INTERNAL_TOKEN,"ENV_CONFIG_CENTER_INTERNAL_TOKEN"))
                .configCenterEnable(resolveBoolean(ENV_CONFIG_CENTER_ENABLE,null))
                .configCenterCacheEnable(resolveBoolean(ENV_CONFIG_CENTER_CACHE_ENABLE,null))
                .configCenterCacheRefreshIntervalSeconds(resolveInteger(ENV_CONFIG_CENTER_CACHE_REFRESH_INTERVAL_SECONDS,60))
                .configCenterTimeout(resolveInteger(ENV_CONFIG_CENTER_TIMEOUT,5))
                .diffServiceAddress(resolve(ENV_DIFF_SERVICE_ADDRESS, "http://localhost:8081"))
                .diffServiceInternalToken(resolve(ENV_DIFF_SERVICE_INTERNAL_TOKEN, "ENV_DIFF_SERVICE_INTERNAL_TOKEN"))
                .diffServiceEnable(resolveBoolean(ENV_DIFF_SERVICE_ENABLE,null))
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
        if (StringUtils.isBlank(value)) {
            return defaultValue;
        }
        return value;
    }

    private static Integer resolveInteger(String key,Integer defaultValue){
        String value = System.getenv(key);
        if (StringUtils.isBlank(value)) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        }catch (NumberFormatException e){
            log.error("[resolveInteger] parse value failed,value={}",value,e);
            return defaultValue;
        }
    }

    private static Boolean resolveBoolean(String key, Boolean defaultValue) {
        String value = System.getenv(key);
        if (StringUtils.isBlank(value)) {
            return defaultValue;
        }
        if(Set.of("true","false").contains(value)) return Objects.equals("true",value);
        return null;
    }
}
