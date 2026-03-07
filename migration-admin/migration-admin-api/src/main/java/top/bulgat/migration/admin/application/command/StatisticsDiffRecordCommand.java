package top.bulgat.migration.admin.application.command;

import java.time.LocalDate;

/**
 * Diff 统计查询命令，包含筛选条件。
 */
public record StatisticsDiffRecordCommand(
        String migrationKey,
        LocalDate startDate,
        LocalDate endDate) {
}
