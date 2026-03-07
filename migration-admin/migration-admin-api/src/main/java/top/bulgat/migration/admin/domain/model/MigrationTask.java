package top.bulgat.migration.admin.domain.model;

import java.time.LocalDateTime;
import lombok.Getter;

/**
 * 迁移任务聚合。
 */
@Getter
public class MigrationTask {

    private final String migrationKey;
    private MigrationStatus status;
    private String description;
    private final LocalDateTime createTime;
    private LocalDateTime updateTime;

    /**
     * 使用当前时间创建任务。
     */
    public MigrationTask(String migrationKey, MigrationStatus status, String description) {
        this(migrationKey, status, description, LocalDateTime.now(), LocalDateTime.now());
    }

    /**
     * 使用指定时间创建任务。
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
     * 变更迁移状态。
     */
    public void changeStatus(MigrationStatus targetStatus) {
        this.status = targetStatus;
        this.updateTime = LocalDateTime.now();
    }

    /**
     * 更新任务描述。
     */
    public void updateDescription(String newDescription) {
        this.description = newDescription;
        this.updateTime = LocalDateTime.now();
    }
}
