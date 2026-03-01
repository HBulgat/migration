package top.bulgat.migration.admin.interfaces.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import top.bulgat.common.base.model.PageResult;
import top.bulgat.common.base.model.Result;
import top.bulgat.migration.admin.application.service.DiffRecordQueryApplicationService;
import top.bulgat.migration.admin.interfaces.assembler.DiffRecordAssembler;
import top.bulgat.migration.admin.interfaces.dto.DiffRecordResponse;
import top.bulgat.migration.admin.interfaces.dto.DiffStatisticsResponse;

/**
 * Diff record query API.
 */
@Tag(name = "Diff Record API", description = "Query diff record list, detail and statistics")
@Validated
@RestController
@RequestMapping("/api/v1/diff_record")
public class DiffRecordController {

    private static final Logger log = LoggerFactory.getLogger(DiffRecordController.class);
    private final DiffRecordQueryApplicationService queryApplicationService;
    private final DiffRecordAssembler assembler;

    public DiffRecordController(
            DiffRecordQueryApplicationService queryApplicationService,
            DiffRecordAssembler assembler) {
        this.queryApplicationService = queryApplicationService;
        this.assembler = assembler;
    }

    @Operation(summary = "获取Diff记录列表")
    @GetMapping("/list")
    public Result<PageResult<DiffRecordResponse>> list(
            @RequestParam("migration_key") @NotBlank String migrationKey,
            @RequestParam(value = "has_diff", required = false) @Min(0) @Max(1) Integer hasDiff,
            @RequestParam(value = "start_date", required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(value = "end_date", required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            @RequestParam(value = "page", defaultValue = "1") @Min(1) int page,
            @RequestParam(value = "page_size", defaultValue = "10") @Min(1) @Max(200) int pageSize) {
        log.info("diff_record.list request migrationKey={}, hasDiff={}, startDate={}, endDate={}, page={}, pageSize={}",
                migrationKey, hasDiff, startDate, endDate, page, pageSize);
        var listCommand = assembler.toListCommand(migrationKey, hasDiff, startDate, endDate, page, pageSize);
        var countCommand = assembler.toCountCommand(migrationKey, hasDiff, startDate, endDate);
        var diffRecordResponses = assembler.toResponseList(queryApplicationService.list(listCommand));
        long total = queryApplicationService.count(countCommand);
        log.info("diff_record.list response migrationKey={}, page={}, pageSize={}, total={}, listSize={}",
                migrationKey, page, pageSize, total, diffRecordResponses.size());
        return Result.success(PageResult.of(
                page,
                pageSize,
                total,
                diffRecordResponses));
    }

    @Operation(summary = "Get diff record detail")
    @GetMapping("/detail")
    public Result<DiffRecordResponse> detail(@RequestParam("id") long id) {
        log.info("diff_record.detail request id={}", id);
        DiffRecordResponse response = assembler.toResponse(queryApplicationService.detail(assembler.toDetailCommand(id)));
        log.info("diff_record.detail response id={}, migrationKey={}, hasDiff={}", response.id(), response.migrationKey(),
                response.hasDiff());
        return Result.success(response);
    }

    @Operation(summary = "获取Diff统计信息")
    @GetMapping("/statistics")
    public Result<DiffStatisticsResponse> statistics(
            @RequestParam("migration_key") @NotBlank String migrationKey,
            @RequestParam(value = "start_date", required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(value = "end_date", required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        log.info("diff_record.statistics request migrationKey={}, startDate={}, endDate={}", migrationKey, startDate, endDate);
        DiffStatisticsResponse response = assembler.toStatisticsResponse(
                queryApplicationService.statistics(assembler.toStatisticsCommand(migrationKey, startDate, endDate)));
        log.info("diff_record.statistics response migrationKey={}, totalCount={}, diffCount={}, diffRate={}",
                migrationKey, response.totalCount(), response.diffCount(), response.diffRate());
        return Result.success(response);
    }
}
