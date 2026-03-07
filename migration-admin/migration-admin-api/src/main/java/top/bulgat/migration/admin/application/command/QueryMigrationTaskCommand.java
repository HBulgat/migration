package top.bulgat.migration.admin.application.command;

/**
 * 迁移任务详情查询命令，包含查询条件。
 */
public record QueryMigrationTaskCommand(String migrationKey) {
}
