package top.bulgat.migration.config.common.model.dataobject;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

public record MigrationTaskConfig(
        @JsonProperty("migration_key") String migrationKey,
        @JsonProperty("status") int status,
        @JsonProperty("description") String description,
        @JsonProperty("create_time") LocalDateTime createTime,
        @JsonProperty("update_time") LocalDateTime updateTime) {
}