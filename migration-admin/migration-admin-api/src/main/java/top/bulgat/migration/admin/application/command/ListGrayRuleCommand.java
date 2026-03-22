package top.bulgat.migration.admin.application.command;

/**
 * 灰度规则列表查询命令，包含筛选与分页参数。
 */
public record ListGrayRuleCommand(
        String migrationKey,
        int page,
        int pageSize) {
}
