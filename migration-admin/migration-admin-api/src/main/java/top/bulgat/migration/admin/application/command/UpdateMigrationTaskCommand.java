package top.bulgat.migration.admin.application.command;

/**
 * 更新迁移任务命令，包含可变更字段。
 */
public record UpdateMigrationTaskCommand(
        String migrationKey,
        Integer status,
        String description) {
}
