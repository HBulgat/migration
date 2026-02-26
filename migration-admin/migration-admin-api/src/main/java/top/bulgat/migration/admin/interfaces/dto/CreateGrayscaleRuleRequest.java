package top.bulgat.migration.admin.interfaces.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotNull;

/**
 * CreateGrayscaleRuleRequest is an API request DTO.
 */
public record CreateGrayscaleRuleRequest(
        @JsonProperty("migration_key")
        @NotBlank
        @Size(max = 128)
        @Pattern(regexp = "^\\S+$") String migrationKey,
        @JsonProperty("rule_type") @NotBlank String ruleType,
        @JsonProperty("rule_value") @NotBlank String ruleValue,
        @JsonProperty("enable") @NotNull Boolean enable) {
}

