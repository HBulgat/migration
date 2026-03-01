package top.bulgat.migration.sdk.starter.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import top.bulgat.migration.sdk.core.config.MigrationSdkProperties;

/**
 * Starter 配置项。
 */
@Data
@ConfigurationProperties(prefix = "migration")
public class MigrationProperties {

        private boolean enabled = true;
        private String configCenterUrl;
        private String diffServiceUrl;
        private Integer defaultTimeout;
        private String internalToken;

        /**
         * 转换为 core sdk 配置。
         *
         * @return core sdk 配置
         */
        public MigrationSdkProperties toSdkProperties() {
                MigrationSdkProperties fromEnv = MigrationSdkProperties.fromEnv();
                return MigrationSdkProperties.builder()
                                .configCenterUrl(configCenterUrl == null || configCenterUrl.isBlank()
                                                ? fromEnv.getConfigCenterUrl()
                                                : configCenterUrl)
                                .diffServiceUrl(diffServiceUrl == null || diffServiceUrl.isBlank()
                                                ? fromEnv.getDiffServiceUrl()
                                                : diffServiceUrl)
                                .defaultTimeout(defaultTimeout == null || defaultTimeout <= 0
                                                ? fromEnv.getDefaultTimeout()
                                                : defaultTimeout)
                                .internalToken(internalToken == null || internalToken.isBlank()
                                                ? fromEnv.getInternalToken()
                                                : internalToken)
                                .build();
        }
}
