package top.bulgat.migration.sdk.starter.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import top.bulgat.migration.sdk.core.config.CachedConfigClient;
import top.bulgat.migration.sdk.core.config.HttpConfigClient;
import top.bulgat.migration.sdk.core.config.MigrationSdkProperties;
import top.bulgat.migration.sdk.core.diff.DisruptorDiffServiceCaller;
import top.bulgat.migration.sdk.core.grayscale.DefaultGrayscaleMatcher;
import top.bulgat.migration.sdk.core.spi.ConfigClient;
import top.bulgat.migration.sdk.core.spi.DiffServiceCaller;
import top.bulgat.migration.sdk.core.spi.GrayscaleMatcher;
import top.bulgat.migration.sdk.core.strategy.MigrationStrategyRegistry;
import top.bulgat.migration.sdk.starter.aop.MigrationAnnotationAdvisor;
import top.bulgat.migration.sdk.starter.aop.MigrationInterceptor;

import java.util.Objects;

/**
 * 迁移 Starter 自动装配。
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({MigrationProperties.class,
        MigrationProperties.DiffServiceClientProperties.class,
        MigrationProperties.ConfigCenterClientProperties.class
})
@ConditionalOnProperty(prefix = "migration", name = "enabled", havingValue = "true", matchIfMissing = true)
public class MigrationAutoConfiguration {

    /**
     * 默认配置客户端。
     */
    @Bean
    @ConditionalOnMissingBean
    public ConfigClient configClient(MigrationProperties properties) {
        MigrationSdkProperties sdkProperties = properties.toSdkProperties();
        ConfigClient configClient = new HttpConfigClient(sdkProperties);
        if (Objects.equals(Boolean.TRUE,sdkProperties.getConfigCenterCacheEnable())){
            return new CachedConfigClient(configClient,sdkProperties.getConfigCenterCacheRefreshIntervalSeconds());
        }
        return configClient;
    }

    /**
     * 默认 Diff 调用器。
     */
    @Bean
    @ConditionalOnMissingBean
    public DiffServiceCaller diffServiceCaller(MigrationProperties properties) {
        return new DisruptorDiffServiceCaller(properties.toSdkProperties());
    }

    /**
     * 默认灰度匹配器。
     */
    @Bean
    @ConditionalOnMissingBean
    public GrayscaleMatcher grayscaleMatcher() {
        return new DefaultGrayscaleMatcher();
    }

    /**
     * 默认策略注册表。
     */
    @Bean
    @ConditionalOnMissingBean
    public MigrationStrategyRegistry migrationStrategyRegistry() {
        return MigrationStrategyRegistry.defaultRegistry();
    }

    /**
     * 注解方法拦截器。
     */
    @Bean
    public MigrationInterceptor migrationInterceptor(
            ConfigClient configClient,
            DiffServiceCaller diffServiceCaller,
            GrayscaleMatcher grayscaleMatcher,
            MigrationStrategyRegistry strategyRegistry,
            MigrationProperties properties) {
        return new MigrationInterceptor(
                configClient, diffServiceCaller, grayscaleMatcher, strategyRegistry, properties);
    }

    /**
     * 注解顾问：将 @Migration 与拦截器绑定。
     */
    @Bean
    public MigrationAnnotationAdvisor migrationAnnotationAdvisor(MigrationInterceptor interceptor) {
        return new MigrationAnnotationAdvisor(interceptor);
    }
}
