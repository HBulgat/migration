package top.bulgat.migration.config.common.dal;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.ibatis.annotations.Mapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.bulgat.migration.config.common.configcenter.ConfigCenterGateway;
import top.bulgat.migration.config.common.model.dataobject.MigrationTaskConfig;

/**
 * 迁移任务配置中心数据访问对象。
 * <p>
 * 统一封装配置中心的读写操作和 DO 序列化/反序列化，含 task index 管理。
 */
@Mapper
public class MigrationTaskConfigDAO {

    private static final Logger log = LoggerFactory.getLogger(MigrationTaskConfigDAO.class);
    private static final String DATA_ID_PREFIX = "migration_task_";
    private static final String INDEX_DATA_ID = "migration_task_index";
    private static final String GROUP = ConfigCenterGateway.DEFAULT_GROUP;

    private final ConfigCenterGateway configCenterGateway;
    private final ObjectMapper objectMapper;

    public MigrationTaskConfigDAO(ConfigCenterGateway configCenterGateway, ObjectMapper objectMapper) {
        this.configCenterGateway = configCenterGateway;
        this.objectMapper = objectMapper;
    }

    /**
     * 根据 migrationKey 查找迁移任务。
     *
     * @param migrationKey 迁移标识
     * @return 任务 DO，不存在时返回空
     */
    public Optional<MigrationTaskConfig> findByMigrationKey(String migrationKey) {
        try {
            var content = configCenterGateway.getConfig(DATA_ID_PREFIX + migrationKey, GROUP);
            if (content.isEmpty() || content.get().isBlank()) {
                return Optional.empty();
            }
            MigrationTaskConfig config = objectMapper.readValue(content.get(), MigrationTaskConfig.class);
            return Optional.ofNullable(config);
        } catch (Exception ex) {
            throw new IllegalStateException(
                    "failed to read migration task for migrationKey: " + migrationKey, ex);
        }
    }

    /**
     * 读取所有迁移任务的 migrationKey 索引。
     *
     * @return migrationKey 列表
     */
    public List<String> getTaskIndex() {
        try {
            var content = configCenterGateway.getConfig(INDEX_DATA_ID, GROUP);
            if (content.isEmpty() || content.get().isBlank()) {
                return new ArrayList<>();
            }
            return objectMapper.readValue(content.get(), new TypeReference<>() {});
        } catch (Exception ex) {
            throw new IllegalStateException("failed to read migration task index", ex);
        }
    }

    /**
     * 保存迁移任务到配置中心，并更新索引。
     *
     * @param config 任务 DO
     */
    public void save(MigrationTaskConfig config) {
        try {
            String content = objectMapper.writeValueAsString(config);
            configCenterGateway.publish(DATA_ID_PREFIX + config.migrationKey(), GROUP, content);
            updateTaskIndex(config.migrationKey(), true);
        } catch (Exception ex) {
            throw new IllegalStateException(
                    "failed to save migration task for migrationKey: " + config.migrationKey(), ex);
        }
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
        try {
            List<String> keys = getTaskIndex();
            if (add && !keys.contains(migrationKey)) {
                keys.add(migrationKey);
            }
            if (!add) {
                keys.remove(migrationKey);
            }
            configCenterGateway.publish(INDEX_DATA_ID, GROUP, objectMapper.writeValueAsString(keys));
        } catch (Exception ex) {
            throw new IllegalStateException("failed to update migration task index", ex);
        }
    }
}
