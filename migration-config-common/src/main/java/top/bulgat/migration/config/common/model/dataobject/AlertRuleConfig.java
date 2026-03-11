package top.bulgat.migration.config.common.model.dataobject;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record AlertRuleConfig(
                @JsonProperty("migration_key") String migrationKey,
                @JsonProperty("rule_id") String ruleId,
                @JsonProperty("name") String name,
                @JsonProperty("enable") boolean enable,
                @JsonProperty("channel") String channel,
                @JsonProperty("template_key") String templateKey,
                @JsonProperty("receivers") List<String> receivers,
                @JsonProperty("create_time") java.time.LocalDateTime createTime,
                @JsonProperty("update_time") java.time.LocalDateTime updateTime) {
}
