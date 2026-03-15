package top.bulgat.migration.admin.interfaces.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Diff 统计响应 DTO，包含时序采样点。
 */
public record DiffStatisticsResponse(
        @JsonProperty("points") List<DiffStatisticsPointResponse> points) {

    public record DiffStatisticsPointResponse(
            @JsonProperty("time_point") String timePoint,
            @JsonProperty("total_count") long totalCount,
            @JsonProperty("diff_count") long diffCount,
            @JsonProperty("diff_rate") double diffRate,
            @JsonProperty("avg_old_cost_time") int avgOldCostTime,
            @JsonProperty("avg_new_cost_time") int avgNewCostTime) {
    }
}
