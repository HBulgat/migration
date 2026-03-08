package top.bulgat.migration.admin.domain.service;

import java.util.Objects;
import org.springframework.stereotype.Component;
import top.bulgat.migration.admin.domain.model.MigrationTask;
import top.bulgat.migration.admin.domain.repository.MigrationTaskRepository;
import top.bulgat.migration.config.common.model.enums.MigrationTaskStatus;

/**
 * 迁移任务领域服务。
 * 负责迁移任务聚合的核心校验规则。
 */
@Component
public class MigrationTaskDomainService {

    /**
     * 校验迁移任务创建请求。
     *
     * @param task 待创建的任务实体
     * @param repository 任务仓储
     */
    public void validateForCreation(MigrationTask task, MigrationTaskRepository repository) {
        if (repository.existsByMigrationKey(task.getMigrationKey())) {
            throw new IllegalArgumentException("migration_key already exists: " + task.getMigrationKey());
        }
    }

    /**
     * 校验迁移状态是否允许流转。
     *
     * @param current 当前状态
     * @param target 目标状态
     */
    public void validateStatusSwitch(MigrationTaskStatus current, MigrationTaskStatus target) {
        if (!current.canSwitchTo(target)) {
            throw new IllegalArgumentException(
                    "invalid status switch, current=" + current.getCode() + ", target=" + target.getCode());
        }
    }

    /**
     * 校验任务描述长度。
     *
     * @param description 任务描述
     */
    public void validateDescription(String description) {
        if (description != null && description.length() > 512) {
            throw new IllegalArgumentException("description is too long");
        }
    }

    /**
     * 校验 migration_key 规范。
     *
     * @param migrationKey 迁移任务标识
     */
    public void validateMigrationKey(String migrationKey) {
        if (migrationKey == null || migrationKey.isBlank()) {
            throw new IllegalArgumentException("migration_key is required");
        }
        if (migrationKey.length() > 128) {
            throw new IllegalArgumentException("migration_key is too long");
        }
        if (Objects.requireNonNull(migrationKey).chars().anyMatch(Character::isWhitespace)) {
            throw new IllegalArgumentException("migration_key must not contain space");
        }
    }
}
