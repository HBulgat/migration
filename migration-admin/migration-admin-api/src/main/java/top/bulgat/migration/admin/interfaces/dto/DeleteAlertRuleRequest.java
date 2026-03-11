package top.bulgat.migration.admin.interfaces.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DeleteAlertRuleRequest {
    @NotBlank(message = "migration_key cannot be blank")
    @JsonProperty("migration_key")
    private String migrationKey;

    @NotBlank(message = "rule_id cannot be blank")
    @JsonProperty("rule_id")
    private String ruleId;
}
