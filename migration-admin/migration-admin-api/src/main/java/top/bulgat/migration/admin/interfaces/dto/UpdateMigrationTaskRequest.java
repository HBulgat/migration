package top.bulgat.migration.admin.interfaces.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * UpdateMigrationTaskRequest is an API request DTO.
 */
public record UpdateMigrationTaskRequest(
        @JsonProperty("migration_key")
        @NotBlank
        @Size(max = 128)
        @Pattern(regexp = "^\\S+$") String migrationKey,
        @JsonProperty("status") @Min(1) @Max(7) Integer status,
        @JsonProperty("description") String description) {
}

