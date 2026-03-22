package top.bulgat.migration.admin.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import top.bulgat.migration.config.common.model.enums.DiffRuleType;

import java.time.LocalDateTime;

/**
 * 迁移差分规则领域模型。
 */
@Getter
public class DiffRule {

    private final String migrationKey;
    private final String ruleId;
    private DiffRuleType ruleType;
    private String fieldPath;
    private String ruleValue;
    private boolean enable;
    private final LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer weight;

    public DiffRule(String migrationKey, String ruleId, DiffRuleType ruleType, String fieldPath, String ruleValue, boolean enable, Integer weight, LocalDateTime createTime, LocalDateTime updateTime) {
        this.migrationKey = migrationKey;
        this.ruleId = ruleId;
        this.ruleType = ruleType;
        this.fieldPath = fieldPath;
        this.ruleValue = ruleValue;
        this.enable = enable;
        this.weight = weight;
        this.createTime = createTime;
        this.updateTime = updateTime;
    }

    public void update(DiffRuleType ruleType, String fieldPath, String ruleValue, Boolean enable, Integer weight) {
        boolean changed = false;
        if (ruleType != null && this.ruleType != ruleType) {
            this.ruleType = ruleType;
            changed = true;
        }
        if (fieldPath != null && !fieldPath.equals(this.fieldPath)) {
            this.fieldPath = fieldPath;
            changed = true;
        }
        if (ruleValue != null && !ruleValue.equals(this.ruleValue)) {
            this.ruleValue = ruleValue;
            changed = true;
        }
        if (enable != null && this.enable != enable) {
            this.enable = enable;
            changed = true;
        }
        if (weight != null && !weight.equals(this.weight)) {
            this.weight = weight;
            changed = true;
        }
        if (changed) {
            this.updateTime = LocalDateTime.now();
        }
    }

    public void changeEnable(boolean enable) {
        if (this.enable != enable) {
            this.enable = enable;
            this.updateTime = LocalDateTime.now();
        }
    }
}
