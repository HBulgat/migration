package top.bulgat.migration.diff.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 领域层 Diff 执行请求。
 */
@Getter
@AllArgsConstructor
public class DiffRequest {
    @JsonProperty("migration_key")
    private final String migrationKey;
    @JsonProperty("trace_id")
    private final String traceId;
    @JsonProperty("old_json")
    private final String oldJson;
    @JsonProperty("new_json")
    private final String newJson;
    @JsonProperty("old_cost_time_ms")
    private final Integer oldCostTimeMs;
    @JsonProperty("new_cost_time_ms")
    private final Integer newCostTimeMs;
    @JsonProperty("gray_param")
    private final String grayParam;
    @JsonProperty("old_success")
    private final Boolean oldSuccess;
    @JsonProperty("new_success")
    private final Boolean newSuccess;
    @JsonProperty("old_error_message")
    private final String oldErrorMessage;
    @JsonProperty("new_error_message")
    private final String newErrorMessage;
    @JsonProperty("old_request_params")
    private final String oldRequestParams;
    @JsonProperty("new_request_params")
    private final String newRequestParams;
    @JsonProperty("migration_task_status")
    private final Integer migrationTaskStatus;
    @JsonProperty("gray_rules")
    private final String grayRules;
    @JsonProperty("gray_hit")
    private final Boolean grayHit;
    @JsonProperty("fallback_triggered")
    private final Boolean fallbackTriggered;
}
