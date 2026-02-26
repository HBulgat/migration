package top.bulgat.migration.admin.interfaces.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

/**
 * MigrationTaskResponse is an API response DTO.
 */
public record MigrationTaskResponse(
        @JsonProperty("migration_key") String migrationKey,
        @JsonProperty("status") int status,
        @JsonProperty("description") String description,
        @JsonProperty("create_time") LocalDateTime createTime,
        @JsonProperty("update_time") LocalDateTime updateTime) {
}
