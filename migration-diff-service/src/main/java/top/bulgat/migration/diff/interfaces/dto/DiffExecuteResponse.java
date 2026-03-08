package top.bulgat.migration.diff.interfaces.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * 接口响应 DTO。
 */
public record DiffExecuteResponse(
        @JsonProperty("has_diff") boolean hasDiff,
        @JsonProperty("diff_results") List<DiffResultItem> diffResults,
        @JsonProperty("cost_time_ms") long costTimeMs) {

    /**
     * 接口响应 DTO。
     */
    public record DiffResultItem(
            @JsonProperty("path") String path,
            @JsonProperty("old_value") String oldValue,
            @JsonProperty("new_value") String newValue,
            @JsonProperty("diff_type") String diffType) {
    }
}
