package top.bulgat.migration.diff.interfaces.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 接口请求 DTO。
 */
@Schema(description = "Diff 执行请求")
public record DiffExecuteRequest(
        @Schema(description = "迁移任务 Key")
        @JsonProperty("migration_key") @NotBlank @Size(max = 128) @Pattern(regexp = "^\\S+$") String migrationKey,
        @Schema(description = "链路追踪 ID")
        @JsonProperty("trace_id") String traceId,
        @Schema(description = "旧接口响应 JSON")
        @JsonProperty("old_json") String oldJson,
        @Schema(description = "新接口响应 JSON")
        @JsonProperty("new_json") String newJson,
        @Schema(description = "旧接口耗时(ms)")
        @JsonProperty("old_cost_time_ms") @Min(0) Integer oldCostTimeMs,
        @Schema(description = "新接口耗时(ms)")
        @JsonProperty("new_cost_time_ms") @Min(0) Integer newCostTimeMs,
        @Schema(description = "灰度参数")
        @JsonProperty("gray_param") String grayParam,
        @Schema(description = "旧接口是否成功")
        @JsonProperty("old_success") Boolean oldSuccess,
        @Schema(description = "新接口是否成功")
        @JsonProperty("new_success") Boolean newSuccess,
        @Schema(description = "旧接口错误信息")
        @JsonProperty("old_error_message") String oldErrorMessage,
        @Schema(description = "新接口错误信息")
        @JsonProperty("new_error_message") String newErrorMessage,
        @Schema(description = "旧接口请求参数")
        @JsonProperty("old_request_params") String oldRequestParams,
        @Schema(description = "新接口请求参数")
        @JsonProperty("new_request_params") String newRequestParams,
        @Schema(description = "迁移任务状态")
        @JsonProperty("migration_status") Integer migrationTaskStatus,
        @Schema(description = "灰度规则集合")
        @JsonProperty("gray_rules") String grayRules,
        @Schema(description = "是否命中灰度")
        @JsonProperty("gray_hit") Boolean grayHit,
        @Schema(description = "是否触发异常降级")
        @JsonProperty("fallback_triggered") Boolean fallbackTriggered) {
}
