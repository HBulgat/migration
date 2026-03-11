package top.bulgat.migration.admin.interfaces.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.util.List;

@Data
public class UpdateAlertRuleRequest {
    @NotBlank(message = "migration_key cannot be blank")
    @JsonProperty("migration_key")
    private String migrationKey;

    @NotBlank(message = "rule_id cannot be blank")
    @JsonProperty("rule_id")
    private String ruleId;

    @JsonProperty("name")
    private String name;

    @JsonProperty("enable")
    private Boolean enable;

    @JsonProperty("channel")
    private String channel;

    @JsonProperty("template_key")
    private String templateKey;

    @JsonProperty("receivers")
    private List<String> receivers;
}
