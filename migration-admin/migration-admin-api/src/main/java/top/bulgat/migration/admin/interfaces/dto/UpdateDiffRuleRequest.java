package top.bulgat.migration.admin.interfaces.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateDiffRuleRequest {
    @NotBlank
    @JsonProperty("migration_key")
    private String migrationKey;

    @NotBlank
    @JsonProperty("rule_id")
    private String ruleId;

    @JsonProperty("rule_type")
    private String ruleType;

    @JsonProperty("field_path")
    private String fieldPath;

    @JsonProperty("rule_value")
    private String ruleValue;

    @JsonProperty("enable")
    private Boolean enable;

    @JsonProperty("weight")
    private Integer weight;
}

