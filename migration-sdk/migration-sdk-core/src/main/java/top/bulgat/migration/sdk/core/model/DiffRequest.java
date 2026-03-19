package top.bulgat.migration.sdk.core.model;

import com.alibaba.fastjson2.annotation.JSONField;
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
    @JSONField(name = "migration_key")
    private String migrationKey;

    @JsonProperty("trace_id")
    @JSONField(name = "trace_id")
    private String traceId;

    @JsonProperty("old_json")
    @JSONField(name = "old_json")
    private String oldJson;

    @JsonProperty("new_json")
    @JSONField(name = "new_json")
    private String newJson;

    @JsonProperty("old_cost_time_ms")
    @JSONField(name = "old_cost_time_ms")
    private Integer oldCostTimeMs;

    @JsonProperty("new_cost_time_ms")
    @JSONField(name = "new_cost_time_ms")
    private Integer newCostTimeMs;

    @JsonProperty("grayscale_param")
    @JSONField(name = "grayscale_param")
    private String grayscaleParam;

    @JsonProperty("old_success")
    @JSONField(name = "old_success")
    private Boolean oldSuccess;

    @JsonProperty("new_success")
    @JSONField(name = "new_success")
    private Boolean newSuccess;

    @JsonProperty("old_error_message")
    @JSONField(name = "old_error_message")
    private String oldErrorMessage;

    @JsonProperty("new_error_message")
    @JSONField(name = "new_error_message")
    private String newErrorMessage;

    @JsonProperty("old_request_params")
    @JSONField(name = "old_request_params")
    private String oldRequestParams;

    @JsonProperty("new_request_params")
    @JSONField(name = "new_request_params")
    private String newRequestParams;

    @JsonProperty("migration_status")
    @JSONField(name = "migration_status")
    private Integer migrationStatus;

    @JsonProperty("grayscale_rules")
    @JSONField(name = "grayscale_rules")
    private String grayscaleRules;

    @JsonProperty("grayscale_hit")
    @JSONField(name = "grayscale_hit")
    private Boolean grayscaleHit;

    @JsonProperty("fallback_triggered")
    @JSONField(name = "fallback_triggered")
    private Boolean fallbackTriggered;
}
