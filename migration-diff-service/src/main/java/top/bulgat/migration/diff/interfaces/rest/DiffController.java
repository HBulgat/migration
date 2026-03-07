package top.bulgat.migration.diff.interfaces.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.bulgat.common.base.model.Result;
import top.bulgat.migration.diff.application.service.DiffApplicationService;
import top.bulgat.migration.diff.interfaces.assembler.DiffCommandAssembler;
import top.bulgat.migration.diff.interfaces.dto.DiffExecuteRequest;
import top.bulgat.migration.diff.interfaces.dto.DiffExecuteResponse;

/**
 * Diff 执行接口。
 */
@Tag(name = "Diff API", description = "Execute JSON diff with configured rules")
@RestController
@RequestMapping("/api/v1/diff")
public class DiffController {

    private final DiffApplicationService applicationService;
    private final DiffCommandAssembler assembler;

    public DiffController(DiffApplicationService applicationService, DiffCommandAssembler assembler) {
        this.applicationService = applicationService;
        this.assembler = assembler;
    }

    /**
     * 执行一次 Diff 请求。
     *
     * @param request Diff 请求参数。
     * @return Diff 执行结果
     */
    @Operation(summary = "Execute diff", description = "Compare old_json and new_json with diff rules")
    @PostMapping
    public Result<DiffExecuteResponse> execute(@Valid @RequestBody DiffExecuteRequest request) {
        return Result.success(assembler.toResponse(applicationService.executeDiff(assembler.toCommand(request))));
    }
}
