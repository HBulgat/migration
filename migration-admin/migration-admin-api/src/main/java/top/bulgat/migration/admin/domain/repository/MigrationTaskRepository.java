package top.bulgat.migration.admin.domain.repository;

import java.util.List;
import java.util.Optional;
import top.bulgat.migration.admin.domain.model.MigrationTask;

/**
 * MigrationTaskRepository 定义持久化访问能力。
 */
public interface MigrationTaskRepository {

    MigrationTask save(MigrationTask task);

    boolean existsByMigrationKey(String migrationKey);

    Optional<MigrationTask> findByMigrationKey(String migrationKey);

    List<MigrationTask> findAll();

    void deleteByMigrationKey(String migrationKey);
}
