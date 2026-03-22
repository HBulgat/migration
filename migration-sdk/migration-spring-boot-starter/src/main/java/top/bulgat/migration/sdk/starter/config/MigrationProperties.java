package top.bulgat.migration.sdk.starter.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import top.bulgat.common.base.util.StringUtils;
import top.bulgat.migration.sdk.core.config.MigrationSdkProperties;

/**
 * Starter 配置项。
 */
@Data
@ConfigurationProperties(prefix = "migration")
@Configuration
public class MigrationProperties {

        private ConfigCenterClientProperties configCenterClient;
        private DiffServiceCallerProperties diffServiceClient;

        @Data
        @ConfigurationProperties(prefix = "migration.config-center-client")
        public static class ConfigCenterClientProperties {
                private boolean enable=true;
                private String address;
                private String internalToken;
                private int timeout=5000;
                private boolean cacheEnable=false;
                private int cacheRefreshIntervalSeconds=30;
        }
        @Data
        @ConfigurationProperties(prefix = "migration.diff-service-caller")
        public static class DiffServiceCallerProperties {
                private boolean enable=true;
                private String address;
                private int timeout=5000;
                private String internalToken;
                private int workerPoolSize=4;
                private int maxConnections=100;
        }

        /**
         * 转换为 核心 SDK 配置。
         *
         * @return 核心 SDK 配置
         */
        public MigrationSdkProperties toSdkProperties() {
                MigrationSdkProperties fromEnv = MigrationSdkProperties.fromEnv();
                return MigrationSdkProperties.builder()
                        .configCenterEnable(configCenterClient == null
                                        ?fromEnv.getConfigCenterEnable()
                                        : configCenterClient.enable)
                        .configCenterAddress(configCenterClient == null || StringUtils.isBlank(configCenterClient.address)
                                        ? fromEnv.getConfigCenterAddress()
                                        : configCenterClient.address)
                        .configCenterInternalToken(configCenterClient ==null||StringUtils.isBlank(configCenterClient.internalToken)
                                        ?fromEnv.getConfigCenterInternalToken()
                                        : configCenterClient.internalToken)
                        .configCenterTimeout(configCenterClient ==null||  configCenterClient.timeout<=0
                                        ? fromEnv.getConfigCenterTimeout()
                                        : configCenterClient.timeout)
                        .configCenterCacheEnable(configCenterClient==null
                                        ?fromEnv.getConfigCenterCacheEnable()
                                        :configCenterClient.cacheEnable)
                        .configCenterCacheRefreshIntervalSeconds(configCenterClient ==null || configCenterClient.cacheRefreshIntervalSeconds<=0
                                        ?fromEnv.getConfigCenterCacheRefreshIntervalSeconds()
                                        : configCenterClient.cacheRefreshIntervalSeconds)
                        .diffServiceEnable(diffServiceClient == null
                                ?fromEnv.getDiffServiceEnable()
                                : diffServiceClient.enable)
                        .diffServiceAddress(diffServiceClient == null || StringUtils.isBlank(diffServiceClient.address)
                                        ? fromEnv.getDiffServiceAddress()
                                        : diffServiceClient.address)
                        .diffServiceInternalToken(diffServiceClient == null || StringUtils.isBlank(diffServiceClient.internalToken)
                                        ? fromEnv.getDiffServiceInternalToken()
                                        : diffServiceClient.internalToken)
                        .diffServiceTimeout(diffServiceClient==null||diffServiceClient.timeout<=0
                                        ? fromEnv.getDiffServiceTimeout()
                                        : diffServiceClient.timeout)
                        .diffServiceWorkerCount(diffServiceClient == null || diffServiceClient.workerPoolSize <= 0
                                        ? fromEnv.getDiffServiceWorkerCount()
                                        : diffServiceClient.workerPoolSize)
                        .diffServiceMaxConnections(diffServiceClient == null || diffServiceClient.maxConnections <= 0
                                        ? fromEnv.getDiffServiceMaxConnections()
                                        : diffServiceClient.maxConnections)
                        .build();
        }
}
