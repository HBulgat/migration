package top.bulgat.migration.diff.interfaces.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 接口响应 DTO。
 */
@Schema(description = "Diff 执行响应")
public record DiffExecuteResponse(
        @Schema(description = "是否存在差异")
        @JsonProperty("has_diff") boolean hasDiff,
        @Schema(description = "Diff 结果明细")
        @JsonProperty("diff_results") List<DiffResultItem> diffResults,
        @Schema(description = "Diff 耗时(ms)")
        @JsonProperty("cost_time_ms") long costTimeMs) {

    /**
     * 接口响应 DTO。
     */
    @Schema(description = "Diff 结果项")
    public record DiffResultItem(
            @Schema(description = "差异 JSON Path")
            @JsonProperty("path") String path,
            @Schema(description = "旧值")
            @JsonProperty("old_value") String oldValue,
            @Schema(description = "新值")
            @JsonProperty("new_value") String newValue,
            @Schema(description = "差异类型")
            @JsonProperty("diff_type") String diffType) {
    }
}
