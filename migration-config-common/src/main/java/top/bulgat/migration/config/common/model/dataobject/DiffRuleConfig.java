package top.bulgat.migration.config.common.model.dataobject;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

public record DiffRuleConfig(
        @JsonProperty("migration_key") String migrationKey,
        @JsonProperty("rule_id") String ruleId,
        @JsonProperty("rule_type") String ruleType,
        @JsonProperty("field_path") String fieldPath,
        @JsonProperty("rule_value") String ruleValue,
        @JsonProperty("enable") boolean enable,
        @JsonProperty("weight") Integer weight,
        @JsonProperty("create_time") LocalDateTime createTime,
        @JsonProperty("update_time") LocalDateTime updateTime) {
}