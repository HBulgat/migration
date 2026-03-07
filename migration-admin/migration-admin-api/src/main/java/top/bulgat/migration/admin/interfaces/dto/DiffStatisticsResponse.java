package top.bulgat.migration.admin.interfaces.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 接口响应 DTO。
 */
public record DiffStatisticsResponse(
        @JsonProperty("total_count") long totalCount,
        @JsonProperty("diff_count") long diffCount,
        @JsonProperty("diff_rate") double diffRate,
        @JsonProperty("avg_old_cost_time") int avgOldCostTime,
        @JsonProperty("avg_new_cost_time") int avgNewCostTime) {
}

