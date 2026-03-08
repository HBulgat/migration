package top.bulgat.migration.diff.domain.model;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 已持久化的 Diff 记录聚合。
 */
public record DiffRecord(long id, String migrationKey, String traceId, String oldResponse, String newResponse,
                         List<DiffItem> diffResults, boolean hasDiff, String diffType, String grayscaleParam,
                         Integer oldCostTimeMs, Integer newCostTimeMs, Integer totalCostTimeMs,
                         LocalDateTime createTime, Boolean oldSuccess, Boolean newSuccess, String oldErrorMessage,
                         String newErrorMessage, String oldRequestParams, String newRequestParams,
                         Integer MigrationTaskStatus, String grayscaleRules, Boolean grayscaleHit,
                         Boolean fallbackTriggered) {

}
