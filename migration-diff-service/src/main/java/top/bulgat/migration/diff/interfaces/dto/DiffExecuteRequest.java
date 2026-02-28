package top.bulgat.migration.diff.interfaces.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * DiffExecuteRequest is an API request DTO.
 */
public record DiffExecuteRequest(
                @JsonProperty("migration_key") @NotBlank @Size(max = 128) @Pattern(regexp = "^\\S+$") String migrationKey,
                @JsonProperty("trace_id") String traceId,
                @JsonProperty("old_json") @NotBlank String oldJson,
                @JsonProperty("new_json") @NotBlank String newJson,
                @JsonProperty("old_cost_time_ms") @Min(0) Integer oldCostTimeMs,
                @JsonProperty("new_cost_time_ms") @Min(0) Integer newCostTimeMs,
                @JsonProperty("grayscale_param") String grayscaleParam) {
}
