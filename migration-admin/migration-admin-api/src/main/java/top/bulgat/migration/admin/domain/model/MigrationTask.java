package top.bulgat.migration.admin.domain.model;

import java.time.LocalDateTime;
import lombok.Getter;

/**
 * Migration task aggregate module.
 */
@Getter
public class MigrationTask {

    private final String migrationKey;
    private MigrationStatus status;
    private String description;
    private final LocalDateTime createTime;
    private LocalDateTime updateTime;

    /**
     * Create task with current timestamps module.
     */
    public MigrationTask(String migrationKey, MigrationStatus status, String description) {
        this(migrationKey, status, description, LocalDateTime.now(), LocalDateTime.now());
    }

    /**
     * Create task with explicit timestamps module.
     */
    public MigrationTask(
            String migrationKey,
            MigrationStatus status,
            String description,
            LocalDateTime createTime,
            LocalDateTime updateTime) {
        this.migrationKey = migrationKey;
        this.status = status;
        this.description = description;
        this.createTime = createTime;
        this.updateTime = updateTime;
    }

    /**
     * Change migration status module.
     */
    public void changeStatus(MigrationStatus targetStatus) {
        this.status = targetStatus;
        this.updateTime = LocalDateTime.now();
    }

    /**
     * Update task description module.
     */
    public void updateDescription(String newDescription) {
        this.description = newDescription;
        this.updateTime = LocalDateTime.now();
    }
}
