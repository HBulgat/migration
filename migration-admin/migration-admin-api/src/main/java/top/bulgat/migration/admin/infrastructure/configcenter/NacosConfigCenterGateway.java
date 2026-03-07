package top.bulgat.migration.admin.infrastructure.configcenter;

import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.config.ConfigService;
import java.util.Optional;
import java.util.Properties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * Nacos配置中心网关能力定义。
 */
@Primary
@Component
public class NacosConfigCenterGateway implements ConfigCenterGateway {

    private final ConfigService configService;

    public NacosConfigCenterGateway(
            @Value("${migration.nacos.server-addr:localhost:8848}") String serverAddr,
            @Value("${migration.nacos.namespace:}") String namespace,
            @Value("${migration.nacos.username:}") String username,
            @Value("${migration.nacos.password:}") String password) throws Exception {
        Properties properties = new Properties();
        properties.setProperty("serverAddr", serverAddr);
        if (namespace != null && !namespace.isBlank()) {
            properties.setProperty("namespace", namespace);
        }
        if (username != null && !username.isBlank()) {
            properties.setProperty("username", username);
        }
        if (password != null && !password.isBlank()) {
            properties.setProperty("password", password);
        }
        this.configService = NacosFactory.createConfigService(properties);
    }

    /**
     * 发布配置内容。
     *
     * @param dataId 配置 dataId。
     * @param group 配置分组。
     * @param content 配置内容。
     */
    @Override
    public void publish(String dataId, String group, String content) {
        try {
            configService.publishConfig(dataId, normalizeGroup(group), content);
        } catch (Exception ex) {
            throw new IllegalStateException("failed to publish config: " + dataId, ex);
        }
    }

    /**
     * 加载配置内容。
     *
     * @param dataId 配置 dataId。
     * @param group 配置分组。
     * @return 返回结果。
     */
    @Override
    public Optional<String> getConfig(String dataId, String group) {
        try {
            String content = configService.getConfig(dataId, normalizeGroup(group), 3000);
            return Optional.ofNullable(content);
        } catch (Exception ex) {
            throw new IllegalStateException("failed to get config: " + dataId, ex);
        }
    }

    /**
     * 按请求删除数据。
     *
     * @param dataId 配置 dataId。
     * @param group 配置分组。
     */
    @Override
    public void delete(String dataId, String group) {
        try {
            configService.removeConfig(dataId, normalizeGroup(group));
        } catch (Exception ex) {
            throw new IllegalStateException("failed to delete config: " + dataId, ex);
        }
    }

    private String normalizeGroup(String group) {
        return (group == null || group.isBlank()) ? DEFAULT_GROUP : group;
    }
}
