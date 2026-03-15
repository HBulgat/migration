package top.bulgat.migration.sdk.starter.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import top.bulgat.common.base.util.StringUtils;
import top.bulgat.migration.sdk.core.config.MigrationSdkProperties;

/**
 * Starter 配置项。
 */
@Data
@ConfigurationProperties(prefix = "migration")
public class MigrationProperties {

        private ConfigCenterProperties configCenterConfig;
        private DiffServiceProperties diffServiceConfig;

        @Data
        @ConfigurationProperties(prefix = "migration.config-center")
        public static class ConfigCenterProperties {
                private boolean enable=true;
                private String address;
                private String internalToken;
                private Integer timeout;
                private Integer cacheRefreshIntervalSeconds;
        }
        @Data
        @ConfigurationProperties(prefix = "migration.diff-service")
        public static class DiffServiceProperties {
                private boolean enable=true;
                private String address;
//                private Integer timeout;
                private String internalToken;
        }

        /**
         * 转换为 核心 SDK 配置。
         *
         * @return 核心 SDK 配置
         */
        public MigrationSdkProperties toSdkProperties() {
                MigrationSdkProperties fromEnv = MigrationSdkProperties.fromEnv();
                return MigrationSdkProperties.builder()
                        .configCenterEnable(configCenterConfig == null
                                        ?fromEnv.getConfigCenterEnable()
                                        : configCenterConfig.enable)
                        .configCenterAddress(configCenterConfig == null || StringUtils.isBlank(configCenterConfig.address)
                                        ? fromEnv.getConfigCenterAddress()
                                        : configCenterConfig.address)
                        .configCenterInternalToken(configCenterConfig==null||StringUtils.isBlank(configCenterConfig.internalToken)
                                        ?fromEnv.getConfigCenterInternalToken()
                                        :configCenterConfig.internalToken)
                        .configCenterTimeout(configCenterConfig==null|| configCenterConfig.timeout==null||configCenterConfig.timeout<=0
                                        ? fromEnv.getConfigCenterTimeout()
                                        : configCenterConfig.timeout)
                        .cacheRefreshIntervalSeconds(configCenterConfig==null||configCenterConfig.cacheRefreshIntervalSeconds==null||configCenterConfig.cacheRefreshIntervalSeconds<=0
                                        ?fromEnv.getCacheRefreshIntervalSeconds()
                                        :configCenterConfig.cacheRefreshIntervalSeconds)
                        .diffServiceEnable(configCenterConfig == null
                                ?fromEnv.getDiffServiceEnable()
                                : diffServiceConfig.enable)
                        .diffServiceAddress(diffServiceConfig == null || StringUtils.isBlank(diffServiceConfig.address)
                                        ? fromEnv.getDiffServiceAddress()
                                        : diffServiceConfig.address)
                        .diffServiceInternalToken(diffServiceConfig == null || StringUtils.isBlank(diffServiceConfig.internalToken)
                                        ? fromEnv.getDiffServiceInternalToken()
                                        : diffServiceConfig.internalToken)
                        .build();
        }
}
