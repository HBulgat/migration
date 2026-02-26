package top.bulgat.migration.admin.interfaces.assembler;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import top.bulgat.migration.admin.application.command.CreateGrayscaleRuleCommand;
import top.bulgat.migration.admin.application.command.DeleteGrayscaleRuleCommand;
import top.bulgat.migration.admin.application.command.ListGrayscaleRuleCommand;
import top.bulgat.migration.admin.application.command.UpdateGrayscaleRuleCommand;
import top.bulgat.migration.admin.application.command.UpdateGrayscaleRuleEnableCommand;
import top.bulgat.migration.admin.domain.model.GrayscaleRule;
import top.bulgat.migration.admin.interfaces.dto.CreateGrayscaleRuleRequest;
import top.bulgat.migration.admin.interfaces.dto.DeleteGrayscaleRuleRequest;
import top.bulgat.migration.admin.interfaces.dto.GrayscaleRuleResponse;
import top.bulgat.migration.admin.interfaces.dto.UpdateGrayscaleRuleEnableRequest;
import top.bulgat.migration.admin.interfaces.dto.UpdateGrayscaleRuleRequest;

/**
 * GrayscaleRuleAssembler converts DTOs and domain models.
 */
@Component
public class GrayscaleRuleAssembler {



    /**
     * Execute toCreateCommand business logic.
     * @param request request payload.
     * @return result value.
     */
    public CreateGrayscaleRuleCommand toCreateCommand(CreateGrayscaleRuleRequest request) {
        return new CreateGrayscaleRuleCommand(
                request.migrationKey(),
                request.ruleType(),
                request.ruleValue(),
                request.enable());
    }

    /**
     * Execute toUpdateCommand business logic.
     * @param request request payload.
     * @return result value.
     */
    public UpdateGrayscaleRuleCommand toUpdateCommand(UpdateGrayscaleRuleRequest request) {
        return new UpdateGrayscaleRuleCommand(
                request.migrationKey(),
                request.ruleId(),
                request.ruleType(),
                request.ruleValue(),
                request.enable());
    }

    /**
     * Execute toDeleteCommand business logic.
     * @param request request payload.
     * @return result value.
     */
    public DeleteGrayscaleRuleCommand toDeleteCommand(DeleteGrayscaleRuleRequest request) {
        return new DeleteGrayscaleRuleCommand(request.migrationKey(), request.ruleId());
    }

    /**
     * Execute toUpdateEnableCommand business logic.
     * @param request request payload.
     * @return result value.
     */
    public UpdateGrayscaleRuleEnableCommand toUpdateEnableCommand(UpdateGrayscaleRuleEnableRequest request) {
        return new UpdateGrayscaleRuleEnableCommand(request.migrationKey(), request.ruleId(), request.enable());
    }

    /**
     * Execute toListCommand business logic.
     * @param migrationKey migration key.
     * @param page page index.
     * @param pageSize page size.
     * @return result value.
     */
    public ListGrayscaleRuleCommand toListCommand(String migrationKey, int page, int pageSize) {
        return new ListGrayscaleRuleCommand(migrationKey, page, pageSize);
    }

    /**
     * Execute toResponse business logic.
     * @param rule rule entity.
     * @return result value.
     */
    public GrayscaleRuleResponse toResponse(GrayscaleRule rule) {
        return new GrayscaleRuleResponse(
                rule.getRuleId(),
                rule.getMigrationKey(),
                rule.getRuleType().name(),
                rule.getRuleValue(),
                rule.isEnable(),
                rule.getCreateTime(),
                rule.getUpdateTime());
    }

    /**
     * Execute toResponseList business logic.
     * @param rules rule collection.
     * @return result value.
     */
    public List<GrayscaleRuleResponse> toResponseList(List<GrayscaleRule> rules) {
        return rules.stream().map(this::toResponse).collect(Collectors.toList());
    }
}

