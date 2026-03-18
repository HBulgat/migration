package top.bulgat.migration.sdk.core.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 迁移任务配置模型。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MigrationConfig {
    @JsonProperty("migration_key")
    private String migrationKey;
    @JsonProperty("status")
    private Integer status;
    @JsonProperty("description")
    private String description;
}
