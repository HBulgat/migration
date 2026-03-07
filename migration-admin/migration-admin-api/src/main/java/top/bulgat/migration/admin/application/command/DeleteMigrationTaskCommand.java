package top.bulgat.migration.admin.application.command;

/**
 * 删除迁移任务命令，包含待删除的迁移标识。
 */
public record DeleteMigrationTaskCommand(String migrationKey) {
}
