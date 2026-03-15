package top.bulgat.migration.admin.application.command;

import java.time.LocalDate;

/**
 * Diff 记录列表查询命令，包含筛选与分页参数。
 */
public record ListDiffRecordCommand(
        String migrationKey,
        Integer hasDiff,
        Integer migrationStatus,
        String traceId,
        LocalDate startDate,
        LocalDate endDate,
        int page,
        int pageSize) {
}
