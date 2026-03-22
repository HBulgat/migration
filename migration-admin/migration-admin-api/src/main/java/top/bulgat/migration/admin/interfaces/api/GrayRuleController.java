package top.bulgat.migration.admin.interfaces.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import top.bulgat.common.base.model.PageResult;
import top.bulgat.common.base.model.Result;
import top.bulgat.migration.admin.application.service.GrayRuleApplicationService;
import top.bulgat.migration.admin.interfaces.assembler.GrayRuleAssembler;
import top.bulgat.migration.admin.interfaces.dto.CreateGrayRuleRequest;
import top.bulgat.migration.admin.interfaces.dto.DeleteGrayRuleRequest;
import top.bulgat.migration.admin.interfaces.dto.GrayRuleResponse;
import top.bulgat.migration.admin.interfaces.dto.UpdateGrayRuleEnableRequest;
import top.bulgat.migration.admin.interfaces.dto.UpdateGrayRuleRequest;

/**
 * 灰度规则管理接口.
 */
@Tag(name = "灰度规则API", description = "管理迁移灰度规则")
@Validated
@RestController
@RequestMapping("/api/v1/gray_rule")
public class GrayRuleController {

    private final GrayRuleApplicationService applicationService;
    private final GrayRuleAssembler assembler;

    public GrayRuleController(
            GrayRuleApplicationService applicationService,
            GrayRuleAssembler assembler) {
        this.applicationService = applicationService;
        this.assembler = assembler;
    }

    @Operation(summary = "创建灰度规则")
    @PostMapping("/create")
    public Result<GrayRuleResponse> create(@Valid @RequestBody CreateGrayRuleRequest request) {
        return Result.success(assembler.toResponse(applicationService.create(assembler.toCreateCommand(request))));
    }

    @Operation(summary = "更新灰度规则")
    @PostMapping("/update")
    public Result<Void> update(@Valid @RequestBody UpdateGrayRuleRequest request) {
        applicationService.update(assembler.toUpdateCommand(request));
        return Result.success(null);
    }

    @Operation(summary = "删除灰度规则")
    @PostMapping("/delete")
    public Result<Void> delete(@Valid @RequestBody DeleteGrayRuleRequest request) {
        applicationService.delete(assembler.toDeleteCommand(request));
        return Result.success(null);
    }

    @Operation(summary = "切换灰度规则启用状态")
    @PostMapping("/update_enable")
    public Result<Void> updateEnable(@Valid @RequestBody UpdateGrayRuleEnableRequest request) {
        applicationService.updateEnable(assembler.toUpdateEnableCommand(request));
        return Result.success(null);
    }

    @Operation(summary = "获取灰度规则列表")
    @GetMapping("/list")
    public Result<PageResult<GrayRuleResponse>> list(
            @RequestParam("migration_key") @NotBlank String migrationKey,
            @RequestParam(value = "page", defaultValue = "1") @Min(1) int page,
            @RequestParam(value = "page_size", defaultValue = "10") @Min(1) @Max(200) int pageSize) {
        var listCommand = assembler.toListCommand(migrationKey, page, pageSize);
        var ruleResponses = assembler.toResponseList(applicationService.list(listCommand));
        return Result.success(PageResult.of(
                page,
                pageSize,
                applicationService.count(listCommand),
                ruleResponses));
    }
}
