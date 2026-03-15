package top.bulgat.migration.admin.interfaces.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class DiffRuleResponse {
    @JsonProperty("migration_key")
    private String migrationKey;

    @JsonProperty("rule_id")
    private String ruleId;

    @JsonProperty("rule_type")
    private String ruleType;

    @JsonProperty("field_path")
    private String fieldPath;

    @JsonProperty("rule_value")
    private String ruleValue;

    @JsonProperty("enable")
    private boolean enable;

    @JsonProperty("weight")
    private Integer weight;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")

    @JsonProperty("create_time")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonProperty("update_time")
    private LocalDateTime updateTime;
}
