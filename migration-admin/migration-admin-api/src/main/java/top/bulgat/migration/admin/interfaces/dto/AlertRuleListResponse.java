package top.bulgat.migration.admin.interfaces.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AlertRuleListResponse {
    @JsonProperty("migration_key")
    private String migrationKey;

    @JsonProperty("rule_id")
    private String ruleId;

    @JsonProperty("name")
    private String name;

    @JsonProperty("enable")
    private Boolean enable;

    @JsonProperty("channel")
    private String channel;

    @JsonProperty("template_key")
    private String templateKey;

    @JsonProperty("receivers")
    private List<String> receivers;

    @JsonProperty("create_time")
    private java.time.LocalDateTime createTime;

    @JsonProperty("update_time")
    private java.time.LocalDateTime updateTime;
}
