package top.bulgat.migration.config.common.configcenter;

import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.exception.NacosException;
import top.bulgat.migration.config.common.config.ConfigCenterProperties;

import java.util.Optional;
import java.util.Properties;

/**
 * Nacos配置中心网关能力定义。
 */
public class NacosConfigCenterGateway implements ConfigCenterGateway {

    private final ConfigService configService;

    public NacosConfigCenterGateway(ConfigCenterProperties configCenterProperties) throws NacosException {
        Properties properties = new Properties();
        properties.setProperty("serverAddr", configCenterProperties.getServerAddr());
        String namespace = configCenterProperties.getMetaInfo().get("namespace");
        if (namespace != null && !namespace.isBlank()) {
            properties.setProperty("namespace", namespace);
        }
        String username = configCenterProperties.getUsername();
        if (username != null && !username.isBlank()) {
            properties.setProperty("username", username);
        }
        String password = configCenterProperties.getPassword();
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
