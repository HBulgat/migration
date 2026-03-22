package top.bulgat.migration.sdk.core.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 灰度规则配置模型。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GrayConfig {
    @JsonProperty("rule_id")
    private String ruleId;
    @JsonProperty("migration_key")
    private String migrationKey;
    @JsonProperty("rule_type")
    private String ruleType;
    @JsonProperty("rule_value")
    private String ruleValue;
    @JsonProperty("enable")
    private Boolean enable;
}

