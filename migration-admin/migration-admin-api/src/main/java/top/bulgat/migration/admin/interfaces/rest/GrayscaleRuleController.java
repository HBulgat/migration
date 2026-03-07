package top.bulgat.migration.admin.interfaces.rest;

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
import top.bulgat.migration.admin.application.service.GrayscaleRuleApplicationService;
import top.bulgat.migration.admin.interfaces.assembler.GrayscaleRuleAssembler;
import top.bulgat.migration.admin.interfaces.dto.CreateGrayscaleRuleRequest;
import top.bulgat.migration.admin.interfaces.dto.DeleteGrayscaleRuleRequest;
import top.bulgat.migration.admin.interfaces.dto.GrayscaleRuleResponse;
import top.bulgat.migration.admin.interfaces.dto.UpdateGrayscaleRuleEnableRequest;
import top.bulgat.migration.admin.interfaces.dto.UpdateGrayscaleRuleRequest;

/**
 * 灰度规则管理接口.
 */
@Tag(name = "Grayscale Rule API", description = "Manage migration 灰度规则")
@Validated
@RestController
@RequestMapping("/api/v1/grayscale_rule")
public class GrayscaleRuleController {

    private final GrayscaleRuleApplicationService applicationService;
    private final GrayscaleRuleAssembler assembler;

    public GrayscaleRuleController(
            GrayscaleRuleApplicationService applicationService,
            GrayscaleRuleAssembler assembler) {
        this.applicationService = applicationService;
        this.assembler = assembler;
    }

    @Operation(summary = "创建灰度规则")
    @PostMapping("/create")
    public Result<GrayscaleRuleResponse> create(@Valid @RequestBody CreateGrayscaleRuleRequest request) {
        return Result.success(assembler.toResponse(applicationService.create(assembler.toCreateCommand(request))));
    }

    @Operation(summary = "更新灰度规则")
    @PostMapping("/update")
    public Result<Void> update(@Valid @RequestBody UpdateGrayscaleRuleRequest request) {
        applicationService.update(assembler.toUpdateCommand(request));
        return Result.success(null);
    }

    @Operation(summary = "删除灰度规则")
    @PostMapping("/delete")
    public Result<Void> delete(@Valid @RequestBody DeleteGrayscaleRuleRequest request) {
        applicationService.delete(assembler.toDeleteCommand(request));
        return Result.success(null);
    }

    @Operation(summary = "Toggle grayscale rule enable status")
    @PostMapping("/update_enable")
    public Result<Void> updateEnable(@Valid @RequestBody UpdateGrayscaleRuleEnableRequest request) {
        applicationService.updateEnable(assembler.toUpdateEnableCommand(request));
        return Result.success(null);
    }

    @Operation(summary = "获取灰度规则列表")
    @GetMapping("/list")
    public Result<PageResult<GrayscaleRuleResponse>> list(
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
