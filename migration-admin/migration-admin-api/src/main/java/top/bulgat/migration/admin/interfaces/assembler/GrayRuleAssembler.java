package top.bulgat.migration.admin.interfaces.assembler;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import top.bulgat.migration.admin.application.command.CreateGrayRuleCommand;
import top.bulgat.migration.admin.application.command.DeleteGrayRuleCommand;
import top.bulgat.migration.admin.application.command.ListGrayRuleCommand;
import top.bulgat.migration.admin.application.command.UpdateGrayRuleCommand;
import top.bulgat.migration.admin.application.command.UpdateGrayRuleEnableCommand;
import top.bulgat.migration.admin.domain.model.GrayRule;
import top.bulgat.migration.admin.interfaces.dto.CreateGrayRuleRequest;
import top.bulgat.migration.admin.interfaces.dto.DeleteGrayRuleRequest;
import top.bulgat.migration.admin.interfaces.dto.GrayRuleResponse;
import top.bulgat.migration.admin.interfaces.dto.UpdateGrayRuleEnableRequest;
import top.bulgat.migration.admin.interfaces.dto.UpdateGrayRuleRequest;

/**
 * 用于转换 DTO 与领域模型。
 */
@Component
public class GrayRuleAssembler {



    /**
     * 执行 toCreateCommand 业务逻辑。
     * @param request 请求参数。
     * @return 返回结果。
     */
    public CreateGrayRuleCommand toCreateCommand(CreateGrayRuleRequest request) {
        return new CreateGrayRuleCommand(
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
    public UpdateGrayRuleCommand toUpdateCommand(UpdateGrayRuleRequest request) {
        return new UpdateGrayRuleCommand(
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
    public DeleteGrayRuleCommand toDeleteCommand(DeleteGrayRuleRequest request) {
        return new DeleteGrayRuleCommand(request.migrationKey(), request.ruleId());
    }

    /**
     * 执行 toUpdateEnableCommand 业务逻辑。
     * @param request 请求参数。
     * @return 返回结果。
     */
    public UpdateGrayRuleEnableCommand toUpdateEnableCommand(UpdateGrayRuleEnableRequest request) {
        return new UpdateGrayRuleEnableCommand(request.migrationKey(), request.ruleId(), request.enable());
    }

    /**
     * 执行 toListCommand 业务逻辑。
     * @param migrationKey 迁移标识。
     * @param page 页码。
     * @param pageSize 每页大小。
     * @return 返回结果。
     */
    public ListGrayRuleCommand toListCommand(String migrationKey, int page, int pageSize) {
        return new ListGrayRuleCommand(migrationKey, page, pageSize);
    }

    /**
     * 执行 toResponse 业务逻辑。
     * @param rule 规则实体。
     * @return 返回结果。
     */
    public GrayRuleResponse toResponse(GrayRule rule) {
        return new GrayRuleResponse(
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
     * @param rules 规则集合。
     * @return 返回结果。
     */
    public List<GrayRuleResponse> toResponseList(List<GrayRule> rules) {
        return rules.stream().map(this::toResponse).collect(Collectors.toList());
    }
}

