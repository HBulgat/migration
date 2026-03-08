package top.bulgat.migration.admin.infrastructure.repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;
import top.bulgat.migration.admin.domain.model.MigrationTask;
import top.bulgat.migration.admin.domain.repository.MigrationTaskRepository;
import top.bulgat.migration.config.common.dal.MigrationTaskConfigDAO;
import top.bulgat.migration.config.common.model.dataobject.MigrationTaskConfig;
import top.bulgat.migration.config.common.model.enums.MigrationTaskStatus;

/**
 * DefaultMigrationTaskRepository 定义持久化访问能力。
 */
@Repository
public class DefaultMigrationTaskRepository implements MigrationTaskRepository {

    private final MigrationTaskConfigDAO migrationTaskConfigDAO;

    public DefaultMigrationTaskRepository(MigrationTaskConfigDAO migrationTaskConfigDAO) {
        this.migrationTaskConfigDAO = migrationTaskConfigDAO;
    }

    @Override
    public MigrationTask save(MigrationTask task) {
        MigrationTaskConfig payload = new MigrationTaskConfig(
                task.getMigrationKey(),
                task.getStatus().getCode(),
                task.getDescription(),
                task.getCreateTime(),
                task.getUpdateTime());
        migrationTaskConfigDAO.save(payload);
        return task;
    }

    @Override
    public boolean existsByMigrationKey(String migrationKey) {
        return findByMigrationKey(migrationKey).isPresent();
    }

    @Override
    public Optional<MigrationTask> findByMigrationKey(String migrationKey) {
        Optional<MigrationTaskConfig> configOpt = migrationTaskConfigDAO.findByMigrationKey(migrationKey);
        return configOpt.map(this::toEntity);
    }

    @Override
    public List<MigrationTask> findAll() {
        List<String> keys = migrationTaskConfigDAO.getTaskIndex();
        List<MigrationTask> tasks = new ArrayList<>();
        for (String key : keys) {
            findByMigrationKey(key).ifPresent(tasks::add);
        }
        return tasks;
    }

    @Override
    public void deleteByMigrationKey(String migrationKey) {
        migrationTaskConfigDAO.delete(migrationKey);
    }

    private MigrationTask toEntity(MigrationTaskConfig config) {
        return new MigrationTask(
                config.migrationKey(),
                MigrationTaskStatus.fromCode(config.status()),
                config.description(),
                config.createTime() == null ? LocalDateTime.now() : config.createTime(),
                config.updateTime() == null ? LocalDateTime.now() : config.updateTime());
    }
}
