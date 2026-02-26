package top.bulgat.migration.admin.infrastructure.configcenter;

import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.config.ConfigService;
import java.util.Optional;
import java.util.Properties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * NacosConfigCenterGateway accesses config center capabilities.
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
     * Publish config content.
     *
     * @param dataId config data id.
     * @param group config group.
     * @param content config content.
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
     * Load config content.
     *
     * @param dataId config data id.
     * @param group config group.
     * @return result value.
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
     * Delete data by request.
     *
     * @param dataId config data id.
     * @param group config group.
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
