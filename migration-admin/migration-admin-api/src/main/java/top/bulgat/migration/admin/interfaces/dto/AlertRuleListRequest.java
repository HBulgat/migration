package top.bulgat.migration.admin.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;

@Data
@Schema(description = "查询告警规则列表请求")
public class AlertRuleListRequest {
    @NotBlank(message = "迁移任务Key不能为空")
    @Schema(description = "迁移任务Key")
    private String migration_key;

    @Schema(description = "通知渠道")
    private String channel;
}
