package top.bulgat.migration.admin.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.env.Environment;
import top.bulgat.migration.config.common.config.ConfigCenterProperties;
import top.bulgat.migration.config.common.configcenter.ConfigCenterGateway;
import top.bulgat.migration.config.common.configcenter.MemoryConfigCenterGateway;
import top.bulgat.migration.config.common.configcenter.NacosConfigCenterGateway;
import top.bulgat.migration.config.common.dal.DiffRuleConfigDAO;
import top.bulgat.migration.config.common.dal.GrayRuleConfigDAO;
import top.bulgat.migration.config.common.dal.MigrationTaskConfigDAO;
import top.bulgat.migration.config.common.model.enums.ConfigCenterType;

@Configuration
public class ConfigCenterConfig {

    @Bean
    @ConfigurationProperties(prefix = "migration-admin.config-center")
    public ConfigCenterProperties configCenterProperties(Environment environment) {
        return Binder.get(environment)
                .bind("migration-admin.config-center", ConfigCenterProperties.class)
                .get();
    }

    @Bean
    @DependsOn("configCenterProperties")
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
