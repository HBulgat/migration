package top.bulgat.migration.admin.application.command;

import lombok.Data;

@Data
public class ListAlertRuleCommand {
    private String migrationKey;
    private String channel;
}
