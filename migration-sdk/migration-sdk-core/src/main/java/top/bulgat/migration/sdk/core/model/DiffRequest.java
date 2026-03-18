package top.bulgat.migration.sdk.core.model;

import com.fasterxml.jackson.annotation.JsonProperty;
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
    @JsonProperty("migration_key")
    private String migrationKey;
    @JsonProperty("trace_id")
    private String traceId;
    @JsonProperty("old_json")
    private String oldJson;
    @JsonProperty("new_json")
    private String newJson;
    @JsonProperty("old_cost_time_ms")
    private Integer oldCostTimeMs;
    @JsonProperty("new_cost_time_ms")
    private Integer newCostTimeMs;
    @JsonProperty("grayscale_param")
    private String grayscaleParam;
    @JsonProperty("old_success")
    private Boolean oldSuccess;
    @JsonProperty("new_success")
    private Boolean newSuccess;
    @JsonProperty("old_error_message")
    private String oldErrorMessage;
    @JsonProperty("new_error_message")
    private String newErrorMessage;
    @JsonProperty("old_request_params")
    private String oldRequestParams;
    @JsonProperty("new_request_params")
    private String newRequestParams;
    @JsonProperty("migration_task_status")
    private Integer migrationTaskStatus;
    @JsonProperty("grayscale_rules")
    private String grayscaleRules;
    @JsonProperty("grayscale_hit")
    private Boolean grayscaleHit;
    @JsonProperty("fallback_triggered")
    private Boolean fallbackTriggered;
}
