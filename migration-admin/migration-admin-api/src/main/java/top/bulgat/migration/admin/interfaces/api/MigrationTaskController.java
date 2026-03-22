package top.bulgat.migration.admin.interfaces.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import top.bulgat.common.base.model.PageResult;
import top.bulgat.common.base.model.Result;
import top.bulgat.migration.admin.application.service.MigrationTaskApplicationService;
import top.bulgat.migration.admin.interfaces.assembler.MigrationTaskCommandAssembler;
import top.bulgat.migration.admin.interfaces.dto.CreateMigrationTaskRequest;
import top.bulgat.migration.admin.interfaces.dto.DeleteMigrationTaskRequest;
import top.bulgat.migration.admin.interfaces.dto.MigrationTaskResponse;
import top.bulgat.migration.admin.interfaces.dto.QueryMigrationTaskRequest;
import top.bulgat.migration.admin.interfaces.dto.UpdateMigrationTaskRequest;
import top.bulgat.migration.admin.interfaces.dto.UpdateMigrationTaskStatusRequest;

/**
 * 迁移任务管理接口。
 */
@Tag(name = "迁移任务API", description = "创建、查询、更新和删除迁移任务")
@Validated
@RestController
@RequestMapping("/api/v1/migration_task")
public class MigrationTaskController {

    private final MigrationTaskApplicationService applicationService;
    private final MigrationTaskCommandAssembler assembler;

    public MigrationTaskController(
            MigrationTaskApplicationService applicationService,
            MigrationTaskCommandAssembler assembler) {
        this.applicationService = applicationService;
        this.assembler = assembler;
    }

    @Operation(summary = "创建迁移任务")
    @PostMapping("/create")
    public Result<MigrationTaskResponse> create(@Valid @RequestBody CreateMigrationTaskRequest request) {
        return Result
                .success(assembler.toResponse(applicationService.createMigrationTask(assembler.toCommand(request))));
    }

    @Operation(summary = "更新迁移任务")
    @PostMapping("/update")
    public Result<Void> update(@Valid @RequestBody UpdateMigrationTaskRequest request) {
        applicationService.updateTask(assembler.toUpdateCommand(request));
        return Result.success(null);
    }

    @Operation(summary = "根据键查询迁移任务")
    @PostMapping("/query")
    public Result<MigrationTaskResponse> query(@Valid @RequestBody QueryMigrationTaskRequest request) {
        return Result
                .success(assembler.toResponse(applicationService.getByMigrationKey(assembler.toQueryCommand(request))));
    }

    @Operation(summary = "根据键删除迁移任务")
    @PostMapping("/delete")
    public Result<Void> delete(@Valid @RequestBody DeleteMigrationTaskRequest request) {
        applicationService.deleteByMigrationKey(assembler.toDeleteCommand(request));
        return Result.success(null);
    }

    @Operation(summary = "获取迁移任务列表")
    @GetMapping("/list")
    public Result<PageResult<MigrationTaskResponse>> list(
            @RequestParam(value = "page", defaultValue = "1") @Min(1) int page,
            @RequestParam(value = "page_size", defaultValue = "10") @Min(1) @Max(200) int pageSize,
            @RequestParam(value = "status", required = false) @Min(1) @Max(7) Integer status,
            @RequestParam(value = "keyword", required = false) String keyword) {
        var listCommand = assembler.toListCommand(status, keyword, page, pageSize);
        var taskResponses = assembler.toResponseList(applicationService.list(listCommand));
        PageResult<MigrationTaskResponse> pageResult = PageResult.of(
                page,
                pageSize,
                applicationService.count(listCommand),
                taskResponses);
        return Result.success(pageResult);
    }

    @Operation(summary = "获取全量迁移任务列表")
    @GetMapping("/list_all")
    public Result<List<MigrationTaskResponse>> listAll() {
        return Result.success(assembler.toResponseList(applicationService.listAll()));
    }

    @Operation(summary = "更新迁移任务状态")
    @PostMapping("/update_status")
    public Result<Void> updateStatus(@Valid @RequestBody UpdateMigrationTaskStatusRequest request) {
        applicationService.updateStatus(assembler.toUpdateStatusCommand(request));
        return Result.success(null);
    }
}
