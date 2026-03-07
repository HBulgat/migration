package top.bulgat.migration.admin.interfaces.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

/**
 * 接口响应 DTO。
 */
public record GrayscaleRuleResponse(
        @JsonProperty("rule_id") String ruleId,
        @JsonProperty("migration_key") String migrationKey,
        @JsonProperty("rule_type") String ruleType,
        @JsonProperty("rule_value") String ruleValue,
        @JsonProperty("enable") boolean enable,
        @JsonProperty("create_time") LocalDateTime createTime,
        @JsonProperty("update_time") LocalDateTime updateTime) {
}

