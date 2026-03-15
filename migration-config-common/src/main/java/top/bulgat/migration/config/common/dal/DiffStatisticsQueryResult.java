package top.bulgat.migration.config.common.dal;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * Diff 统计查询结果。
 */
@Data
public class DiffStatisticsQueryResult {
    /**
     * 时间点。
     */
    private LocalDateTime timePoint;
    /**
     * 总数。
     */
    private Long totalCount;
    /**
     * 不一致数。
     */
    private Long diffCount;
    /**
     * 旧接口平均耗时。
     */
    private Double avgOldCost;
    /**
     * 新接口平均耗时。
     */
    private Double avgNewCost;
}
