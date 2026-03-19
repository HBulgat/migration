package top.bulgat.migration.sdk.starter.config;

import org.springframework.lang.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.ReflectionUtils;
import top.bulgat.migration.sdk.core.spi.ConfigClient;
import top.bulgat.migration.sdk.starter.annotation.Migration;

import java.util.HashSet;
import java.util.Set;

/**
 * 迁移配置预热器。
 * 在 Spring 容器启动完成后，自动扫描所有 @Migration 注解并拉取配置，
 * 避免第一个业务请求由于懒加载配置而导致的延迟尖峰。
 */
public class MigrationPreWarmer implements ApplicationListener<ContextRefreshedEvent> {

    private static final Logger log = LoggerFactory.getLogger(MigrationPreWarmer.class);

    private final ConfigClient configClient;

    public MigrationPreWarmer(ConfigClient configClient) {
        this.configClient = configClient;
    }

    @Override
    public void onApplicationEvent(@NonNull ContextRefreshedEvent event) {
        ApplicationContext applicationContext = event.getApplicationContext();
        // 确保只在根容器处理
        if (applicationContext.getParent() != null) {
            return;
        }

        log.info("[Migration-SDK] Starting migration configuration pre-warming...");
        
        Set<String> keys = new HashSet<>();
        String[] beanNames = applicationContext.getBeanDefinitionNames();

        for (String beanName : beanNames) {
            if (beanName == null) continue;
            Object bean = null;
            try {
                bean = applicationContext.getBean(beanName);
            } catch (Exception e) {
                // 部分 Bean 可能无法实例化，忽略
                continue;
            }

            Class<?> targetClass = bean.getClass();
            ReflectionUtils.doWithMethods(targetClass, method -> {
                Migration migration = AnnotationUtils.findAnnotation(method, Migration.class);
                if (migration != null) {
                    keys.add(migration.key());
                }
            });
        }

        if (keys.isEmpty()) {
            log.info("[Migration-SDK] No @Migration keys found for pre-warming.");
            return;
        }

        log.info("[Migration-SDK] Found {} migration keys to pre-warm: {}", keys.size(), keys);

        for (String key : keys) {
            try {
                // 触发同步加载并注入缓存
                configClient.getMigrationConfig(key);
                configClient.getGrayscaleRules(key);
                log.info("[Migration-SDK] Pre-warmed configuration for key: {}", key);
            } catch (Exception e) {
                log.warn("[Migration-SDK] Failed to pre-warm configuration for key: {}", key, e);
            }
        }
        
        log.info("[Migration-SDK] Migration configuration pre-warming completed.");
    }
}
