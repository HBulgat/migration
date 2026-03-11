package top.bulgat.migration.admin.application.command;

import lombok.Data;

@Data
public class DeleteAlertRuleCommand {
    private String migrationKey;
    private String ruleId;
}
