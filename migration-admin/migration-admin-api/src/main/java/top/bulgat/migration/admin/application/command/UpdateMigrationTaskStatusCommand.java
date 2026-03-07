package top.bulgat.migration.admin.application.command;

/**
 * 迁移任务状态变更命令，包含目标状态。
 */
public record UpdateMigrationTaskStatusCommand(
        String migrationKey,
        int targetStatus) {
}
