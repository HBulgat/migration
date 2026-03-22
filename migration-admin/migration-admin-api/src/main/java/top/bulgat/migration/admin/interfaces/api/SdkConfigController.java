package top.bulgat.migration.admin.interfaces.api;

import io.swagger.v3.oas.annotations.Hidden;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;

import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import top.bulgat.common.base.model.Result;
import top.bulgat.common.base.util.JsonUtils;
import top.bulgat.migration.admin.application.service.GrayRuleApplicationService;
import top.bulgat.migration.admin.application.service.MigrationTaskApplicationService;
import top.bulgat.migration.admin.interfaces.assembler.GrayRuleAssembler;
import top.bulgat.migration.admin.interfaces.assembler.MigrationTaskCommandAssembler;
import top.bulgat.migration.admin.interfaces.dto.GrayRuleResponse;
import top.bulgat.migration.admin.interfaces.dto.MigrationTaskResponse;
import top.bulgat.migration.admin.interfaces.dto.QueryMigrationTaskRequest;

@Slf4j
@Hidden
@Validated
@RestController
@RequestMapping("/api/internal/sdk")
public class SdkConfigController {
    private final MigrationTaskApplicationService migrationTaskApplicationService;
    private final MigrationTaskCommandAssembler migrationTaskAssembler;
    private final GrayRuleApplicationService grayRuleApplicationService;
    private final GrayRuleAssembler grayRuleAssembler;

    public SdkConfigController(
            MigrationTaskApplicationService migrationTaskApplicationService,
            MigrationTaskCommandAssembler migrationTaskAssembler,
            GrayRuleApplicationService grayRuleApplicationService,
            GrayRuleAssembler grayRuleAssembler) {
        this.migrationTaskApplicationService = migrationTaskApplicationService;
        this.migrationTaskAssembler = migrationTaskAssembler;
        this.grayRuleApplicationService = grayRuleApplicationService;
        this.grayRuleAssembler = grayRuleAssembler;
    }

    @PostMapping("/migration_task/query")
    public Result<MigrationTaskResponse> queryMigrationTask(@Valid @RequestBody QueryMigrationTaskRequest request) {
        return Result.success(migrationTaskAssembler.toResponse(
                migrationTaskApplicationService.getByMigrationKey(migrationTaskAssembler.toQueryCommand(request))));
    }

    @GetMapping("/gray_rule/list")
    public Result<List<GrayRuleResponse>> listGrayRules(
            @RequestParam("migration_key") @NotBlank String migrationKey) {
        log.info("[listGrayRules] migrationKey={}", migrationKey);
        List<GrayRuleResponse> res = grayRuleAssembler.toResponseList(
                grayRuleApplicationService.listAllByMigrationKey(migrationKey));
        log.info("[listGrayRules] res={}", JsonUtils.toJson(res));
        return Result.success(res);
    }
}
