package top.bulgat.migration.diff.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import top.bulgat.migration.config.common.config.ConfigCenterProperties;
import top.bulgat.migration.config.common.configcenter.ConfigCenterGateway;
import top.bulgat.migration.config.common.configcenter.MemoryConfigCenterGateway;
import top.bulgat.migration.config.common.configcenter.NacosConfigCenterGateway;
import top.bulgat.migration.config.common.model.enums.ConfigCenterType;

@Configuration
public class ConfigCenterConfig {

    @Bean
    @ConfigurationProperties(prefix = "migration-diff.config-center")
    public ConfigCenterProperties configCenterProperties() {
        return new ConfigCenterProperties();
    }

    @Bean
    public ConfigCenterGateway configCenterGateway(ConfigCenterProperties configCenterProperties) throws Exception {
        ConfigCenterType configCenterType = configCenterProperties.getType();
        switch (configCenterType) {
            case MEMORY -> {
                return new MemoryConfigCenterGateway();
            }
            case NACOS -> {
                if (configCenterType.isSupported()) {
                    return new NacosConfigCenterGateway(configCenterProperties);
                }
                throw new IllegalArgumentException("NACOS配置中心不支持");
            }
            default -> throw new IllegalArgumentException(String.format("%s配置中心不支持", configCenterType));
        }
    }


}
