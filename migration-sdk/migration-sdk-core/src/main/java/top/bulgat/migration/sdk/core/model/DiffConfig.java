package top.bulgat.migration.sdk.core.model;

import com.fasterxml.jackson.annotation.JsonProperty;
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
    @JsonProperty("migration_key")
    private String migrationKey;
    /** Rule type: IGNORE/TOLERANCE/SCRIPT/SORT. */
    @JsonProperty("rule_type")
    private String ruleType;
    /** Field path (supports JSONPath). */
    @JsonProperty("field_path")
    private String fieldPath;
    /** Rule value. */
    @JsonProperty("rule_value")
    private String ruleValue;
    /** 规则是否启用。 */
    @JsonProperty("enable")
    private Boolean enable;
    /** 权重值。 */
    @JsonProperty("weight")
    private Integer weight;
}
