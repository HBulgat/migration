package top.bulgat.migration.diff.interfaces.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 接口请求 DTO。
 */
public record DiffExecuteRequest(
        @JsonProperty("migration_key") @NotBlank @Size(max = 128) @Pattern(regexp = "^\\S+$") String migrationKey,
        @JsonProperty("trace_id") String traceId,
        @JsonProperty("old_json") String oldJson,
        @JsonProperty("new_json") String newJson,
        @JsonProperty("old_cost_time_ms") @Min(0) Integer oldCostTimeMs,
        @JsonProperty("new_cost_time_ms") @Min(0) Integer newCostTimeMs,
        @JsonProperty("grayscale_param") String grayscaleParam,
        @JsonProperty("old_success") Boolean oldSuccess,
        @JsonProperty("new_success") Boolean newSuccess,
        @JsonProperty("old_error_message") String oldErrorMessage,
        @JsonProperty("new_error_message") String newErrorMessage,
        @JsonProperty("old_request_params") String oldRequestParams,
        @JsonProperty("new_request_params") String newRequestParams,
        @JsonProperty("migration_status") Integer migrationStatus,
        @JsonProperty("grayscale_rules") String grayscaleRules,
        @JsonProperty("grayscale_hit") Boolean grayscaleHit,
        @JsonProperty("fallback_triggered") Boolean fallbackTriggered) {
}
