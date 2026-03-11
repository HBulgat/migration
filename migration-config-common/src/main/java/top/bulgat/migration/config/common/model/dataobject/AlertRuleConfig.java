package top.bulgat.migration.config.common.model.dataobject;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;


public record AlertRuleConfig(
        @JsonProperty("migration_key") String migrationKey,
        @JsonProperty("name") String name,
        @JsonProperty("enable") boolean enable,
        @JsonProperty("channel") String channel,
        @JsonProperty("template_key") String templateKey,
        @JsonProperty("receivers") List<String> receivers
) {}
