package top.bulgat.migration.admin.interfaces.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotNull;

/**
 * UpdateGrayscaleRuleEnableRequest is an API request DTO.
 */
public record UpdateGrayscaleRuleEnableRequest(
        @JsonProperty("migration_key")
        @NotBlank
        @Size(max = 128)
        @Pattern(regexp = "^\\S+$") String migrationKey,
        @JsonProperty("rule_id") @NotBlank String ruleId,
        @JsonProperty("enable") @NotNull Boolean enable) {
}

