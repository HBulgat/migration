package top.bulgat.migration.admin.application.command;

/**
 * 迁移任务列表查询命令，包含筛选与分页参数。
 */
public record ListMigrationTaskCommand(
        Integer status,
        String keyword,
        int page,
        int pageSize) {
}
