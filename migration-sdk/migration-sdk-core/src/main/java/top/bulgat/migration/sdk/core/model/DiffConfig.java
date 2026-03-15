package top.bulgat.migration.sdk.core.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Diff 规则配置模型。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiffConfig {

    /** 迁移任务唯一标识. */
    private String migrationKey;
    /** Rule type: IGNORE/TOLERANCE/SCRIPT/SORT. */
    private String ruleType;
    /** Field path (supports JSONPath). */
    private String fieldPath;
    /** Rule value. */
    private String ruleValue;
    /** 规则是否启用。 */
    private Boolean enable;
    /** 权重值。 */
    private Integer weight;
}
