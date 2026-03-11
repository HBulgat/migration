package top.bulgat.migration.admin.application.command;

import lombok.Data;
import java.util.List;

@Data
public class UpdateAlertRuleCommand {
    private String migrationKey;
    private String ruleId;
    private String name;
    private Boolean enable;
    private String channel;
    private String templateKey;
    private List<String> receivers;
}
