package top.bulgat.migration.admin.domain.model;

import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 管理端读侧使用的 Diff 记录聚合。
 */
@Getter
@AllArgsConstructor
public class DiffRecord {

    private final long id;
    private final String migrationKey;
    private final String traceId;
    private final String oldResponse;
    private final String newResponse;
    private final List<DiffItem> diffResults;
    private final boolean hasDiff;
    private final String diffType;
    private final String grayscaleParam;
    private final Integer oldCostTimeMs;
    private final Integer newCostTimeMs;
    private final Integer totalCostTimeMs;
    private final LocalDateTime createTime;
    private final Boolean oldSuccess;
    private final Boolean newSuccess;
    private final String oldErrorMessage;
    private final String newErrorMessage;
    private final String oldRequestParams;
    private final String newRequestParams;
    private final Integer migrationStatus;
    private final String grayscaleRules;
    private final Boolean grayscaleHit;
    private final Boolean fallbackTriggered;
}
