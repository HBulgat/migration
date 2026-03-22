package top.bulgat.migration.config.common.dal;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.ibatis.annotations.Mapper;
import top.bulgat.common.base.util.JsonUtils;
import top.bulgat.common.base.util.StringUtils;
import top.bulgat.migration.config.common.configcenter.ConfigCenterGateway;
import top.bulgat.migration.config.common.model.dataobject.MigrationTaskConfig;

/**
 * 迁移任务配置中心数据访问对象。
 * <p>
 * 统一封装配置中心的读写操作和 DO 序列化/反序列化，含 task index 管理。
 */
@Mapper
public class MigrationTaskConfigDAO {

    private static final String DATA_ID_PREFIX = "migration_task_";
    private static final String INDEX_DATA_ID = "migration_task_index";
    private static final String GROUP = ConfigCenterGateway.DEFAULT_GROUP;

    private final ConfigCenterGateway configCenterGateway;

    public MigrationTaskConfigDAO(ConfigCenterGateway configCenterGateway) {
        this.configCenterGateway = configCenterGateway;
    }

    /**
     * 根据 migrationKey 查找迁移任务。
     *
     * @param migrationKey 迁移标识
     * @return 任务 DO，不存在时返回空
     */
    public Optional<MigrationTaskConfig> findByMigrationKey(String migrationKey) {
        var content = configCenterGateway.getConfig(DATA_ID_PREFIX + migrationKey, GROUP);
        if (content.isEmpty() || StringUtils.isBlank(content.get())) {
            return Optional.empty();
        }
        MigrationTaskConfig config = JsonUtils.fromJson(content.get(), MigrationTaskConfig.class);
        return Optional.ofNullable(config);
    }

    /**
     * 读取所有迁移任务的 migrationKey 索引。
     *
     * @return migrationKey 列表
     */
    public List<String> getTaskIndex() {
        var content = configCenterGateway.getConfig(INDEX_DATA_ID, GROUP);
        if (content.isEmpty() || StringUtils.isBlank(content.get())) {
            return new ArrayList<>();
        }
        return JsonUtils.toList(content.get(),String.class);

    }

    /**
     * 保存迁移任务到配置中心，并更新索引。
     *
     * @param config 任务 DO
     */
    public void save(MigrationTaskConfig config) {
        String content = JsonUtils.toJson(config);
        configCenterGateway.publish(DATA_ID_PREFIX + config.migrationKey(), GROUP, content);
        updateTaskIndex(config.migrationKey(), true);
    }

    /**
     * 删除迁移任务，并从索引中移除。
     *
     * @param migrationKey 迁移标识
     */
    public void delete(String migrationKey) {
        configCenterGateway.delete(DATA_ID_PREFIX + migrationKey, GROUP);
        updateTaskIndex(migrationKey, false);
    }

    private void updateTaskIndex(String migrationKey, boolean add) {
        List<String> keys = getTaskIndex();
        if (add && !keys.contains(migrationKey)) {
            keys.add(migrationKey);
        }
        if (!add) {
            keys.remove(migrationKey);
        }
        configCenterGateway.publish(INDEX_DATA_ID, GROUP, JsonUtils.toJson(keys));
    }
}
