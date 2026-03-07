package top.bulgat.migration.admin.infrastructure.configcenter;

import java.util.Optional;

/**
 * 配置中心网关能力定义。
 */
public interface ConfigCenterGateway {

    String DEFAULT_GROUP = "DEFAULT_GROUP";

    default void publish(String dataId, String content) {
        publish(dataId, DEFAULT_GROUP, content);
    }

    void publish(String dataId, String group, String content);

    default Optional<String> getConfig(String dataId) {
        return getConfig(dataId, DEFAULT_GROUP);
    }

    Optional<String> getConfig(String dataId, String group);

    default void delete(String dataId) {
        delete(dataId, DEFAULT_GROUP);
    }

    void delete(String dataId, String group);
}
