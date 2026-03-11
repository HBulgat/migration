package top.bulgat.migration.admin.application.command;

import lombok.Data;

@Data
public class UpdateAlertRuleEnableCommand {
    private String migrationKey;
    private String ruleId;
    private Boolean enable;
}
