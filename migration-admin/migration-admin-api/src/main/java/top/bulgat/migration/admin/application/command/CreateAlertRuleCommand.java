package top.bulgat.migration.admin.application.command;

import lombok.Data;
import java.util.List;

@Data
public class CreateAlertRuleCommand {
    private String migrationKey;
    private String name;
    private Boolean enable;
    private String channel;
    private String templateKey;
    private List<String> receivers;
}
