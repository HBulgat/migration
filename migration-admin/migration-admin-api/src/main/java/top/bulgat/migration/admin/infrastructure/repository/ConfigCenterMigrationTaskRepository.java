package top.bulgat.migration.admin.infrastructure.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;
import top.bulgat.migration.admin.domain.model.MigrationStatus;
import top.bulgat.migration.admin.domain.model.MigrationTask;
import top.bulgat.migration.admin.domain.repository.MigrationTaskRepository;
import top.bulgat.migration.admin.infrastructure.configcenter.ConfigCenterGateway;

/**
 * ConfigCenterMigrationTaskRepository 定义持久化访问能力。
 */
@Primary
@Repository
public class ConfigCenterMigrationTaskRepository implements MigrationTaskRepository {

    private static final String TASK_DATA_ID_PREFIX = "migration_";
    private static final String TASK_INDEX_DATA_ID = "migration_index";
    private static final String TASK_GROUP = "MIGRATION_TASK_GROUP";

    private final ConfigCenterGateway configCenterGateway;
    private final ObjectMapper objectMapper;

    public ConfigCenterMigrationTaskRepository(ConfigCenterGateway configCenterGateway, ObjectMapper objectMapper) {
        this.configCenterGateway = configCenterGateway;
        this.objectMapper = objectMapper;
    }

    /**
     * 持久化数据。
     * @param task 任务实体。
     * @return 返回结果。
     */
    @Override
    public MigrationTask save(MigrationTask task) {
        TaskConfig payload = new TaskConfig(
                task.getMigrationKey(),
                task.getStatus().getCode(),
                task.getDescription(),
                task.getCreateTime(),
                task.getUpdateTime());
        try {
            configCenterGateway.publish(
                    TASK_DATA_ID_PREFIX + task.getMigrationKey(),
                    TASK_GROUP,
                    objectMapper.writeValueAsString(payload));
            updateTaskIndex(task.getMigrationKey(), true);
            return task;
        } catch (Exception ex) {
            throw new IllegalStateException("failed to save migration task", ex);
        }
    }

    /**
     * 执行 existsByMigrationKey 业务逻辑。
     * @param migrationKey 迁移标识。
     * @return 返回结果。
     */
    @Override
    public boolean existsByMigrationKey(String migrationKey) {
        return findByMigrationKey(migrationKey).isPresent();
    }

    /**
     * 执行 findByMigrationKey 业务逻辑。
     * @param migrationKey 迁移标识。
     * @return 返回结果。
     */
    @Override
    public Optional<MigrationTask> findByMigrationKey(String migrationKey) {
        Optional<String> content = configCenterGateway.getConfig(TASK_DATA_ID_PREFIX + migrationKey, TASK_GROUP);
        if (content.isEmpty() || content.get().isBlank()) {
            content = configCenterGateway.getConfig(
                    TASK_DATA_ID_PREFIX + migrationKey,
                    ConfigCenterGateway.DEFAULT_GROUP);
        }
        return content.filter(value -> !value.isBlank()).map(this::deserializeTask);
    }

    /**
     * 执行 findAll 业务逻辑。
     * @return 返回结果。
     */
    @Override
    public List<MigrationTask> findAll() {
        List<String> keys = readTaskIndex();
        List<MigrationTask> tasks = new ArrayList<>();
        for (String key : keys) {
            findByMigrationKey(key).ifPresent(tasks::add);
        }
        return tasks;
    }

    /**
     * 执行 deleteByMigrationKey 业务逻辑。
     * @param migrationKey 迁移标识。
     */
    @Override
    public void deleteByMigrationKey(String migrationKey) {
        configCenterGateway.delete(TASK_DATA_ID_PREFIX + migrationKey, TASK_GROUP);
        configCenterGateway.delete(TASK_DATA_ID_PREFIX + migrationKey, ConfigCenterGateway.DEFAULT_GROUP);
        updateTaskIndex(migrationKey, false);
    }

    private MigrationTask deserializeTask(String content) {
        try {
            TaskConfig config = objectMapper.readValue(content, TaskConfig.class);
            return new MigrationTask(
                    config.migrationKey(),
                    MigrationStatus.fromCode(config.status()),
                    config.description(),
                    config.createTime() == null ? LocalDateTime.now() : config.createTime(),
                    config.updateTime() == null ? LocalDateTime.now() : config.updateTime());
        } catch (Exception ex) {
            throw new IllegalStateException("failed to deserialize migration task", ex);
        }
    }

    private List<String> readTaskIndex() {
        try {
            Optional<String> indexContent = configCenterGateway.getConfig(TASK_INDEX_DATA_ID, TASK_GROUP);
            if (indexContent.isEmpty() || indexContent.get().isBlank()) {
                indexContent = configCenterGateway.getConfig(TASK_INDEX_DATA_ID, ConfigCenterGateway.DEFAULT_GROUP);
            }
            if (indexContent.isEmpty() || indexContent.get().isBlank()) {
                return new ArrayList<>();
            }
            return objectMapper.readValue(indexContent.get(), new TypeReference<List<String>>() {
            });
        } catch (Exception ex) {
            throw new IllegalStateException("failed to read migration task index", ex);
        }
    }

    private void updateTaskIndex(String migrationKey, boolean add) {
        try {
            List<String> keys = readTaskIndex();
            if (add && !keys.contains(migrationKey)) {
                keys.add(migrationKey);
            }
            if (!add) {
                keys.remove(migrationKey);
            }
            configCenterGateway.publish(TASK_INDEX_DATA_ID, TASK_GROUP, objectMapper.writeValueAsString(keys));
        } catch (Exception ex) {
            throw new IllegalStateException("failed to update migration task index", ex);
        }
    }

    private record TaskConfig(
            @JsonProperty("migration_key") String migrationKey,
            @JsonProperty("status") int status,
            @JsonProperty("description") String description,
            @JsonProperty("create_time") LocalDateTime createTime,
            @JsonProperty("update_time") LocalDateTime updateTime) {
    }
}
