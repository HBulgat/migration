package top.bulgat.migration.admin.interfaces.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateAlertRuleEnableRequest {
    @NotBlank(message = "migration_key cannot be blank")
    @JsonProperty("migration_key")
    private String migrationKey;

    @NotBlank(message = "rule_id cannot be blank")
    @JsonProperty("rule_id")
    private String ruleId;

    @NotNull(message = "enable cannot be null")
    @JsonProperty("enable")
    private Boolean enable;
}
