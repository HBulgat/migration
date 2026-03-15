package top.bulgat.migration.admin.application.command;

import java.time.LocalDateTime;
import top.bulgat.migration.admin.domain.model.StatisticsGranularity;

/**
 * Diff 统计查询命令，包含筛选条件、状态和粒度。
 */
public record StatisticsDiffRecordCommand(
        String migrationKey,
        LocalDateTime startDate,
        LocalDateTime endDate,
        Integer migrationStatus,
        StatisticsGranularity granularity) {
}
