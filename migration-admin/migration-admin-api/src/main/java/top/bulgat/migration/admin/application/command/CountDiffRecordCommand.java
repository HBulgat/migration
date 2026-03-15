package top.bulgat.migration.admin.application.command;

import java.time.LocalDate;

/**
 * Diff 记录计数命令，包含统计筛选条件。
 */
public record CountDiffRecordCommand(
        String migrationKey,
        Integer hasDiff,
        Integer migrationStatus,
        String traceId,
        LocalDate startDate,
        LocalDate endDate) {
}
