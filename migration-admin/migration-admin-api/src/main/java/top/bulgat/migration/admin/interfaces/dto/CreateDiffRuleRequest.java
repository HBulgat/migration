package top.bulgat.migration.admin.interfaces.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateDiffRuleRequest {
    @NotBlank
    @JsonProperty("migration_key")
    private String migrationKey;

    @NotBlank
    @JsonProperty("rule_type")
    private String ruleType;

    @NotBlank
    @JsonProperty("field_path")
    private String fieldPath;

    @JsonProperty("rule_value")
    private String ruleValue;

    @NotNull
    @JsonProperty("enable")
    private Boolean enable;
}
