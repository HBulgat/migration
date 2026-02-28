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
 * GrayscaleRuleAssembler 用于转换DTO和领域模型。
 */
@Component
public class GrayscaleRuleAssembler {



    /**
     * 执行 toCreateCommand 业务逻辑。
     * @param request 请求参数。
     * @return 返回结果。
     */
    public CreateGrayscaleRuleCommand toCreateCommand(CreateGrayscaleRuleRequest request) {
        return new CreateGrayscaleRuleCommand(
                request.migrationKey(),
                request.ruleType(),
                request.ruleValue(),
                request.enable());
    }

    /**
     * 执行 toUpdateCommand 业务逻辑。
     * @param request 请求参数。
     * @return 返回结果。
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
     * 执行 toDeleteCommand 业务逻辑。
     * @param request 请求参数。
     * @return 返回结果。
     */
    public DeleteGrayscaleRuleCommand toDeleteCommand(DeleteGrayscaleRuleRequest request) {
        return new DeleteGrayscaleRuleCommand(request.migrationKey(), request.ruleId());
    }

    /**
     * 执行 toUpdateEnableCommand 业务逻辑。
     * @param request 请求参数。
     * @return 返回结果。
     */
    public UpdateGrayscaleRuleEnableCommand toUpdateEnableCommand(UpdateGrayscaleRuleEnableRequest request) {
        return new UpdateGrayscaleRuleEnableCommand(request.migrationKey(), request.ruleId(), request.enable());
    }

    /**
     * 执行 toListCommand 业务逻辑。
     * @param migrationKey migration key.
     * @param page 页码。
     * @param pageSize 每页大小。
     * @return 返回结果。
     */
    public ListGrayscaleRuleCommand toListCommand(String migrationKey, int page, int pageSize) {
        return new ListGrayscaleRuleCommand(migrationKey, page, pageSize);
    }

    /**
     * 执行 toResponse 业务逻辑。
     * @param rule rule entity.
     * @return 返回结果。
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
     * 执行 toResponseList 业务逻辑。
     * @param rules rule collection.
     * @return 返回结果。
     */
    public List<GrayscaleRuleResponse> toResponseList(List<GrayscaleRule> rules) {
        return rules.stream().map(this::toResponse).collect(Collectors.toList());
    }
}

