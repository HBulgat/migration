package top.bulgat.migration.diff.domain.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 领域层 Diff 执行请求。
 */
@Getter
@AllArgsConstructor
public class DiffRequest {

    private final String migrationKey;
    private final String traceId;
    private final String oldJson;
    private final String newJson;
    private final Integer oldCostTimeMs;
    private final Integer newCostTimeMs;
    private final String grayParam;
    private final Boolean oldSuccess;
    private final Boolean newSuccess;
    private final String oldErrorMessage;
    private final String newErrorMessage;
    private final String oldRequestParams;
    private final String newRequestParams;
    private final Integer migrationTaskStatus;
    private final String grayRules;
    private final Boolean grayHit;
    private final Boolean fallbackTriggered;
}
