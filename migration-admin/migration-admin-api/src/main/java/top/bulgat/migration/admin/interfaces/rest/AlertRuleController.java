package top.bulgat.migration.admin.interfaces.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import top.bulgat.common.base.model.Result;
import top.bulgat.migration.admin.application.service.AlertRuleApplicationService;
import top.bulgat.migration.admin.domain.model.AlertRule;
import top.bulgat.migration.admin.interfaces.assembler.AlertRuleAssembler;
import top.bulgat.migration.admin.interfaces.dto.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/alert-rules")
@Tag(name = "告警规则管理", description = "告警规则增删改查相关接口")
public class AlertRuleController {

    private final AlertRuleApplicationService alertRuleApplicationService;
    private final AlertRuleAssembler assembler;

    public AlertRuleController(AlertRuleApplicationService alertRuleApplicationService,
                               AlertRuleAssembler assembler) {
        this.alertRuleApplicationService = alertRuleApplicationService;
        this.assembler = assembler;
    }

    @PostMapping("/create")
    @Operation(summary = "创建告警规则")
    public Result<CreateAlertRuleResponse> createAlertRule(@Validated @RequestBody CreateAlertRuleRequest request) {
        String ruleId = alertRuleApplicationService.createAlertRule(assembler.toCommand(request));
        return Result.success(new CreateAlertRuleResponse(ruleId));
    }

    @PostMapping("/update")
    @Operation(summary = "更新告警规则")
    public Result<Boolean> updateAlertRule(@Validated @RequestBody UpdateAlertRuleRequest request) {
        alertRuleApplicationService.updateAlertRule(assembler.toCommand(request));
        return Result.success(true);
    }

    @PostMapping("/update-enable")
    @Operation(summary = "更新告警规则状态")
    public Result<Boolean> updateAlertRuleEnable(@Validated @RequestBody UpdateAlertRuleEnableRequest request) {
        alertRuleApplicationService.updateAlertRuleEnable(assembler.toCommand(request));
        return Result.success(true);
    }

    @PostMapping("/delete")
    @Operation(summary = "删除告警规则")
    public Result<Boolean> deleteAlertRule(@Validated @RequestBody DeleteAlertRuleRequest request) {
        alertRuleApplicationService.deleteAlertRule(assembler.toCommand(request));
        return Result.success(true);
    }

    @GetMapping("/list")
    @Operation(summary = "获取告警规则列表")
    public Result<List<AlertRuleListResponse>> listAlertRules(@RequestParam("migration_key") String migrationKey) {
        List<AlertRule> rules = alertRuleApplicationService.listAlertRules(assembler.toCommand(migrationKey));
        return Result.success(assembler.toDtoList(rules));
    }
}
