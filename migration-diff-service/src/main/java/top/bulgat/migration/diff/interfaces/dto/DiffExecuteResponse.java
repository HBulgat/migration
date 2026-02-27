package top.bulgat.migration.diff.interfaces.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * DiffExecuteResponse is an API response DTO.
 */
public record DiffExecuteResponse(
        @JsonProperty("has_diff") boolean hasDiff,
        @JsonProperty("diff_results") List<DiffItemResponse> diffResults,
        @JsonProperty("cost_time_ms") long costTimeMs) {

    /**
     * DiffItemResponse is an API response DTO.
     */
    public record DiffItemResponse(
            @JsonProperty("path") String path,
            @JsonProperty("old_value") String oldValue,
            @JsonProperty("new_value") String newValue,
            @JsonProperty("diff_type") String diffType) {
    }
}
