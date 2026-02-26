package top.bulgat.migration.admin.interfaces.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateDiffRuleEnableRequest {
    @NotBlank
    @JsonProperty("migration_key")
    private String migrationKey;

    @NotBlank
    @JsonProperty("rule_id")
    private String ruleId;

    @NotNull
    @JsonProperty("enable")
    private Boolean enable;
}
