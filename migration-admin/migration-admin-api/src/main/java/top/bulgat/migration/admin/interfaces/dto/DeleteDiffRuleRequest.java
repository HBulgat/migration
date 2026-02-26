package top.bulgat.migration.admin.interfaces.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DeleteDiffRuleRequest {
    @NotBlank
    @JsonProperty("migration_key")
    private String migrationKey;

    @NotBlank
    @JsonProperty("rule_id")
    private String ruleId;
}
