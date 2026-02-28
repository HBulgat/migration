package top.bulgat.migration.admin.infrastructure.repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;
import top.bulgat.migration.admin.domain.model.MigrationTask;
import top.bulgat.migration.admin.domain.repository.MigrationTaskRepository;

/**
 * InMemoryMigrationTaskRepository defines persistence access.
 */
@Repository
public class InMemoryMigrationTaskRepository implements MigrationTaskRepository {

    private final Map<String, MigrationTask> tasks = new ConcurrentHashMap<>();

    /**
     * Persist data.
     * @param task 任务实体。
     * @return 返回结果。
     */
    @Override
    public MigrationTask save(MigrationTask task) {
        tasks.put(task.getMigrationKey(), task);
        return task;
    }

    /**
     * 执行 existsByMigrationKey 业务逻辑。
     * @param migrationKey migration key.
     * @return 返回结果。
     */
    @Override
    public boolean existsByMigrationKey(String migrationKey) {
        return tasks.containsKey(migrationKey);
    }

    /**
     * 执行 findByMigrationKey 业务逻辑。
     * @param migrationKey migration key.
     * @return 返回结果。
     */
    @Override
    public Optional<MigrationTask> findByMigrationKey(String migrationKey) {
        return Optional.ofNullable(tasks.get(migrationKey));
    }

    /**
     * 执行 findAll 业务逻辑。
     * @return 返回结果。
     */
    @Override
    public List<MigrationTask> findAll() {
        return new ArrayList<>(tasks.values());
    }

    /**
     * 执行 deleteByMigrationKey 业务逻辑。
     * @param migrationKey migration key.
     */
    @Override
    public void deleteByMigrationKey(String migrationKey) {
        tasks.remove(migrationKey);
    }
}
