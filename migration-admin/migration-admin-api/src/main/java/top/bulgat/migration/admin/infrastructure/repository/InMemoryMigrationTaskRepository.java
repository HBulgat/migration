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
     * @param task task entity.
     * @return result value.
     */
    @Override
    public MigrationTask save(MigrationTask task) {
        tasks.put(task.getMigrationKey(), task);
        return task;
    }

    /**
     * Execute existsByMigrationKey business logic.
     * @param migrationKey migration key.
     * @return result value.
     */
    @Override
    public boolean existsByMigrationKey(String migrationKey) {
        return tasks.containsKey(migrationKey);
    }

    /**
     * Execute findByMigrationKey business logic.
     * @param migrationKey migration key.
     * @return result value.
     */
    @Override
    public Optional<MigrationTask> findByMigrationKey(String migrationKey) {
        return Optional.ofNullable(tasks.get(migrationKey));
    }

    /**
     * Execute findAll business logic.
     * @return result value.
     */
    @Override
    public List<MigrationTask> findAll() {
        return new ArrayList<>(tasks.values());
    }

    /**
     * Execute deleteByMigrationKey business logic.
     * @param migrationKey migration key.
     */
    @Override
    public void deleteByMigrationKey(String migrationKey) {
        tasks.remove(migrationKey);
    }
}
