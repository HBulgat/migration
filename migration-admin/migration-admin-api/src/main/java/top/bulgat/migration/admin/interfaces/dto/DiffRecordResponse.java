package top.bulgat.migration.admin.interfaces.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DiffRecordResponse is an API response DTO.
 */
public record DiffRecordResponse(
        @JsonProperty("id") long id,
        @JsonProperty("migration_key") String migrationKey,
        @JsonProperty("trace_id") String traceId,
        @JsonProperty("old_response") String oldResponse,
        @JsonProperty("new_response") String newResponse,
        @JsonProperty("diff_results") List<DiffItemResponse> diffResults,
        @JsonProperty("has_diff") boolean hasDiff,
        @JsonProperty("diff_type") String diffType,
        @JsonProperty("grayscale_param") String grayscaleParam,
        @JsonProperty("old_cost_time_ms") Integer oldCostTimeMs,
        @JsonProperty("new_cost_time_ms") Integer newCostTimeMs,
        @JsonProperty("total_cost_time_ms") Integer totalCostTimeMs,
        @JsonProperty("create_time") LocalDateTime createTime) {
}

