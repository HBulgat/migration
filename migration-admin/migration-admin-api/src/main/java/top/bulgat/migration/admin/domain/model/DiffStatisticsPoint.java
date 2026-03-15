package top.bulgat.migration.admin.domain.model;

import java.time.LocalDateTime;

/**
 * Diff 时序统计点。
 */
public record DiffStatisticsPoint(
        LocalDateTime timePoint,
        long totalCount,
        long diffCount,
        double diffRate,
        int avgOldCostTime,
        int avgNewCostTime) {
}
