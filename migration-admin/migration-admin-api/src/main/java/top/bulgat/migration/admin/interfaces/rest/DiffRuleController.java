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
import top.bulgat.common.model.PageResult;
import top.bulgat.common.model.Result;
import top.bulgat.migration.admin.application.service.DiffRuleApplicationService;
import top.bulgat.migration.admin.interfaces.assembler.DiffRuleAssembler;
import top.bulgat.migration.admin.interfaces.dto.CreateDiffRuleRequest;
import top.bulgat.migration.admin.interfaces.dto.DeleteDiffRuleRequest;
import top.bulgat.migration.admin.interfaces.dto.DiffRuleResponse;
import top.bulgat.migration.admin.interfaces.dto.UpdateDiffRuleEnableRequest;
import top.bulgat.migration.admin.interfaces.dto.UpdateDiffRuleRequest;

/**
 * Diff rule management API.
 */
@Tag(name = "Diff Rule API", description = "Manage migration diff rules")
@Validated
@RestController
@RequestMapping("/api/v1/diff_rule")
public class DiffRuleController {

    private final DiffRuleApplicationService applicationService;
    private final DiffRuleAssembler assembler;

    public DiffRuleController(
            DiffRuleApplicationService applicationService,
            DiffRuleAssembler assembler) {
        this.applicationService = applicationService;
        this.assembler = assembler;
    }

    @Operation(summary = "Create diff rule")
    @PostMapping("/create")
    public Result<DiffRuleResponse> create(@Valid @RequestBody CreateDiffRuleRequest request) {
        return Result.success(assembler.toResponse(applicationService.create(assembler.toCreateCommand(request))));
    }

    @Operation(summary = "Update diff rule")
    @PostMapping("/update")
    public Result<Void> update(@Valid @RequestBody UpdateDiffRuleRequest request) {
        applicationService.update(assembler.toUpdateCommand(request));
        return Result.success(null);
    }

    @Operation(summary = "Delete diff rule")
    @PostMapping("/delete")
    public Result<Void> delete(@Valid @RequestBody DeleteDiffRuleRequest request) {
        applicationService.delete(assembler.toDeleteCommand(request));
        return Result.success(null);
    }

    @Operation(summary = "Toggle diff rule enable status")
    @PostMapping("/update_enable")
    public Result<Void> updateEnable(@Valid @RequestBody UpdateDiffRuleEnableRequest request) {
        applicationService.updateEnable(assembler.toUpdateEnableCommand(request));
        return Result.success(null);
    }

    @Operation(summary = "List diff rules")
    @GetMapping("/list")
    public Result<PageResult<DiffRuleResponse>> list(
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
