package top.bulgat.migration.sdk.core.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Diff 请求模型，SDK 上报给 diff 服务使用。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiffRequest {

    private String migrationKey;
    private String traceId;
    private String oldJson;
    private String newJson;
    private Integer oldCostTimeMs;
    private Integer newCostTimeMs;
    private String grayscaleParam;
    private Boolean oldSuccess;
    private Boolean newSuccess;
    private String oldErrorMessage;
    private String newErrorMessage;
    private String oldRequestParams;
    private String newRequestParams;
    private Integer MigrationTaskStatus;
    private String grayscaleRules;
    private Boolean grayscaleHit;
    private Boolean fallbackTriggered;
}
