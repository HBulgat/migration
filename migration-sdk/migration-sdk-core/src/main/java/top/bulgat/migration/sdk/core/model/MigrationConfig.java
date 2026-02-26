package top.bulgat.migration.sdk.core.model;

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

    private String migrationKey;
    private Integer status;
    private String description;
    private Integer timeout;
}
