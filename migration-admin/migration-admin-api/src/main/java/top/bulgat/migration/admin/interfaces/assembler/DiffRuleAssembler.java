package top.bulgat.migration.admin.interfaces.assembler;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import top.bulgat.migration.admin.application.command.CreateDiffRuleCommand;
import top.bulgat.migration.admin.application.command.DeleteDiffRuleCommand;
import top.bulgat.migration.admin.application.command.ListDiffRuleCommand;
import top.bulgat.migration.admin.application.command.UpdateDiffRuleCommand;
import top.bulgat.migration.admin.application.command.UpdateDiffRuleEnableCommand;
import top.bulgat.migration.admin.domain.model.DiffRule;
import top.bulgat.migration.admin.interfaces.dto.CreateDiffRuleRequest;
import top.bulgat.migration.admin.interfaces.dto.DeleteDiffRuleRequest;
import top.bulgat.migration.admin.interfaces.dto.DiffRuleResponse;
import top.bulgat.migration.admin.interfaces.dto.UpdateDiffRuleEnableRequest;
import top.bulgat.migration.admin.interfaces.dto.UpdateDiffRuleRequest;

@Component
public class DiffRuleAssembler {

    public CreateDiffRuleCommand toCreateCommand(CreateDiffRuleRequest request) {
        return new CreateDiffRuleCommand(
                request.getMigrationKey(),
                request.getRuleType(),
                request.getFieldPath(),
                request.getRuleValue(),
                request.getEnable(),
                request.getWeight());
    }


    public UpdateDiffRuleCommand toUpdateCommand(UpdateDiffRuleRequest request) {
        return new UpdateDiffRuleCommand(
                request.getMigrationKey(),
                request.getRuleId(),
                request.getRuleType(),
                request.getFieldPath(),
                request.getRuleValue(),
                request.getEnable(),
                request.getWeight());
    }


    public UpdateDiffRuleEnableCommand toUpdateEnableCommand(UpdateDiffRuleEnableRequest request) {
        return new UpdateDiffRuleEnableCommand(
                request.getMigrationKey(),
                request.getRuleId(),
                request.getEnable());
    }

    public DeleteDiffRuleCommand toDeleteCommand(DeleteDiffRuleRequest request) {
        return new DeleteDiffRuleCommand(request.getMigrationKey(), request.getRuleId());
    }

    public ListDiffRuleCommand toListCommand(String migrationKey, int page, int pageSize) {
        return new ListDiffRuleCommand(migrationKey, page, pageSize);
    }

    public DiffRuleResponse toResponse(DiffRule rule) {
        if (rule == null) {
            return null;
        }
        DiffRuleResponse resp = new DiffRuleResponse();
        resp.setMigrationKey(rule.getMigrationKey());
        resp.setRuleId(rule.getRuleId());
        resp.setRuleType(rule.getRuleType().name());
        resp.setFieldPath(rule.getFieldPath());
        resp.setRuleValue(rule.getRuleValue());
        resp.setEnable(rule.isEnable());
        resp.setWeight(rule.getWeight());
        resp.setCreateTime(rule.getCreateTime());
        resp.setUpdateTime(rule.getUpdateTime());
        return resp;
    }

    public List<DiffRuleResponse> toResponseList(List<DiffRule> rules) {
        return rules.stream().map(this::toResponse).collect(Collectors.toList());
    }
}
