package top.bulgat.migration.admin.application.command;

/**
 * 创建迁移任务应用命令。
 */
public record CreateMigrationTaskCommand(String migrationKey, int status, String description) {
}
