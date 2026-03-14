package top.bulgat.migration.admin.interfaces.assembler;

import org.springframework.stereotype.Component;

import top.bulgat.migration.admin.interfaces.dto.*;
import top.bulgat.migration.admin.application.command.*;
import top.bulgat.migration.admin.domain.model.AlertRule;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class AlertRuleAssembler {

    public CreateAlertRuleCommand toCommand(CreateAlertRuleRequest request) {
        CreateAlertRuleCommand cmd = new CreateAlertRuleCommand();
        cmd.setMigrationKey(request.getMigrationKey());
        cmd.setName(request.getName());
        cmd.setEnable(request.getEnable());
        cmd.setChannel(request.getChannel());
        cmd.setTemplateKey(request.getTemplateKey());
        cmd.setReceivers(request.getReceivers());
        return cmd;
    }

    public UpdateAlertRuleCommand toCommand(UpdateAlertRuleRequest request) {
        UpdateAlertRuleCommand cmd = new UpdateAlertRuleCommand();
        cmd.setMigrationKey(request.getMigrationKey());
        cmd.setRuleId(request.getRuleId());
        cmd.setName(request.getName());
        cmd.setEnable(request.getEnable());
        cmd.setChannel(request.getChannel());
        cmd.setTemplateKey(request.getTemplateKey());
        cmd.setReceivers(request.getReceivers());
        return cmd;
    }

    public UpdateAlertRuleEnableCommand toCommand(UpdateAlertRuleEnableRequest request) {
        UpdateAlertRuleEnableCommand cmd = new UpdateAlertRuleEnableCommand();
        cmd.setMigrationKey(request.getMigrationKey());
        cmd.setRuleId(request.getRuleId());
        cmd.setEnable(request.getEnable());
        return cmd;
    }

    public DeleteAlertRuleCommand toCommand(DeleteAlertRuleRequest request) {
        DeleteAlertRuleCommand cmd = new DeleteAlertRuleCommand();
        cmd.setMigrationKey(request.getMigrationKey());
        cmd.setRuleId(request.getRuleId());
        return cmd;
    }
    
    public ListAlertRuleCommand toCommand(String migrationKey) {
        ListAlertRuleCommand cmd = new ListAlertRuleCommand();
        cmd.setMigrationKey(migrationKey);
        return cmd;
    }

    public ListAlertRuleCommand toCommand(AlertRuleListRequest request) {
        ListAlertRuleCommand cmd = new ListAlertRuleCommand();
        cmd.setMigrationKey(request.getMigration_key());
        cmd.setChannel(request.getChannel());
        return cmd;
    }

    public AlertRuleListResponse toDto(AlertRule rule) {
        AlertRuleListResponse dto = new AlertRuleListResponse();
        dto.setMigrationKey(rule.getMigrationKey());
        dto.setRuleId(rule.getRuleId());
        dto.setName(rule.getName());
        dto.setEnable(rule.isEnable());
        dto.setChannel(rule.getChannel().name());
        dto.setTemplateKey(rule.getTemplateKey());
        dto.setReceivers(rule.getReceivers());
        dto.setCreateTime(rule.getCreateTime());
        dto.setUpdateTime(rule.getUpdateTime());
        return dto;
    }

    public List<AlertRuleListResponse> toDtoList(List<AlertRule> rules) {
        return rules.stream().map(this::toDto).collect(Collectors.toList());
    }
}
