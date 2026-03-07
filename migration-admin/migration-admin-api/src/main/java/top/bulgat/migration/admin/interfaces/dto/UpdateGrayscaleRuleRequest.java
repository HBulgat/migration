package top.bulgat.migration.admin.interfaces.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 接口请求 DTO。
 */
public record UpdateGrayscaleRuleRequest(
        @JsonProperty("migration_key")
        @NotBlank
        @Size(max = 128)
        @Pattern(regexp = "^\\S+$") String migrationKey,
        @JsonProperty("rule_id") @NotBlank String ruleId,
        @JsonProperty("rule_type") String ruleType,
        @JsonProperty("rule_value") String ruleValue,
        @JsonProperty("enable") Boolean enable) {
}

