package top.bulgat.migration.admin.infrastructure.configcenter;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * InMemory配置中心网关能力定义。
 */
@Component
public class InMemoryConfigCenterGateway implements ConfigCenterGateway {

    private final Map<String, String> configs = new ConcurrentHashMap<>();

    /**
     * 发布配置内容。
     *
     * @param dataId 配置 dataId。
     * @param group 配置分组。
     * @param content 配置内容。
     */
    @Override
    public void publish(String dataId, String group, String content) {
        configs.put(buildKey(dataId, group), content);
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
        return Optional.ofNullable(configs.get(buildKey(dataId, group)));
    }

    /**
     * 按请求删除数据。
     *
     * @param dataId 配置 dataId。
     * @param group 配置分组。
     */
    @Override
    public void delete(String dataId, String group) {
        configs.remove(buildKey(dataId, group));
    }

    private String buildKey(String dataId, String group) {
        String effectiveGroup = (group == null || group.isBlank()) ? DEFAULT_GROUP : group;
        return effectiveGroup + ':' + dataId;
    }
}
