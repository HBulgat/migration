package top.bulgat.migration.admin.interfaces.api;

import io.swagger.v3.oas.annotations.Hidden;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import top.bulgat.common.base.model.Result;
import top.bulgat.migration.admin.application.service.GrayscaleRuleApplicationService;
import top.bulgat.migration.admin.application.service.MigrationTaskApplicationService;
import top.bulgat.migration.admin.interfaces.assembler.GrayscaleRuleAssembler;
import top.bulgat.migration.admin.interfaces.assembler.MigrationTaskCommandAssembler;
import top.bulgat.migration.admin.interfaces.dto.GrayscaleRuleResponse;
import top.bulgat.migration.admin.interfaces.dto.MigrationTaskResponse;
import top.bulgat.migration.admin.interfaces.dto.QueryMigrationTaskRequest;

@Hidden
@Validated
@RestController
@RequestMapping("/api/internal/sdk")
public class SdkConfigController {
    private final MigrationTaskApplicationService migrationTaskApplicationService;
    private final MigrationTaskCommandAssembler migrationTaskAssembler;
    private final GrayscaleRuleApplicationService grayscaleRuleApplicationService;
    private final GrayscaleRuleAssembler grayscaleRuleAssembler;

    public SdkConfigController(
            MigrationTaskApplicationService migrationTaskApplicationService,
            MigrationTaskCommandAssembler migrationTaskAssembler,
            GrayscaleRuleApplicationService grayscaleRuleApplicationService,
            GrayscaleRuleAssembler grayscaleRuleAssembler) {
        this.migrationTaskApplicationService = migrationTaskApplicationService;
        this.migrationTaskAssembler = migrationTaskAssembler;
        this.grayscaleRuleApplicationService = grayscaleRuleApplicationService;
        this.grayscaleRuleAssembler = grayscaleRuleAssembler;
    }

    @PostMapping("/migration_task/query")
    public Result<MigrationTaskResponse> queryMigrationTask(@Valid @RequestBody QueryMigrationTaskRequest request) {
        return Result.success(migrationTaskAssembler.toResponse(
                migrationTaskApplicationService.getByMigrationKey(migrationTaskAssembler.toQueryCommand(request))));
    }

    @GetMapping("/grayscale_rule/list")
    public Result<List<GrayscaleRuleResponse>> listGrayscaleRules(
            @RequestParam("migration_key") @NotBlank String migrationKey) {
        return Result.success(grayscaleRuleAssembler.toResponseList(
                grayscaleRuleApplicationService.listAllByMigrationKey(migrationKey)));
    }
}
