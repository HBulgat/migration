package top.bulgat.migration.admin.interfaces.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.bulgat.common.base.model.PageResult;
import top.bulgat.common.base.model.Result;
import top.bulgat.migration.admin.application.AlertTemplateApplicationService;
import top.bulgat.migration.admin.interfaces.dto.AlertTemplateDTO;
import top.bulgat.migration.admin.interfaces.dto.CreateAlertTemplateRequest;
import top.bulgat.migration.admin.interfaces.dto.UpdateAlertTemplateRequest;

@RestController
@RequestMapping("/api/v1/alert-templates")
@Tag(name = "消息模板管理接口", description = "消息模板的增、改、查管理")
public class AlertTemplateController {

    private final AlertTemplateApplicationService alertTemplateApplicationService;

    public AlertTemplateController(AlertTemplateApplicationService alertTemplateApplicationService) {
        this.alertTemplateApplicationService = alertTemplateApplicationService;
    }

    @PostMapping("/create")
    @Operation(summary = "创建消息模板")
    public Result<Void> createAlertTemplate(@RequestBody @Validated CreateAlertTemplateRequest request) {
        alertTemplateApplicationService.createAlertTemplate(request);
        return Result.success();
    }

    @PostMapping("/update")
    @Operation(summary = "更新消息模板")
    public Result<Void> updateAlertTemplate(@RequestBody @Validated UpdateAlertTemplateRequest request) {
        alertTemplateApplicationService.updateAlertTemplate(request);
        return Result.success();
    }

    @PostMapping("/list")
    @Operation(summary = "查询消息模板列表", description = "获取所有可用的消息模板")
    public Result<PageResult<AlertTemplateDTO>> listAlertTemplates(@org.springframework.web.bind.annotation.RequestParam(required = false) String channel) {
        // We use PageResult for consistency with other listing APIs, even though there's no actual pagination.
        return Result.success(alertTemplateApplicationService.listAlertTemplates(channel));
    }
}
