package top.bulgat.migration.admin.infrastructure.configcenter;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * InMemoryConfigCenterGateway accesses config center capabilities.
 */
@Component
public class InMemoryConfigCenterGateway implements ConfigCenterGateway {

    private final Map<String, String> configs = new ConcurrentHashMap<>();

    /**
     * Publish config content.
     *
     * @param dataId config data id.
     * @param group config group.
     * @param content config content.
     */
    @Override
    public void publish(String dataId, String group, String content) {
        configs.put(buildKey(dataId, group), content);
    }

    /**
     * Load config content.
     *
     * @param dataId config data id.
     * @param group config group.
     * @return 返回结果。
     */
    @Override
    public Optional<String> getConfig(String dataId, String group) {
        return Optional.ofNullable(configs.get(buildKey(dataId, group)));
    }

    /**
     * Delete data by request.
     *
     * @param dataId config data id.
     * @param group config group.
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
