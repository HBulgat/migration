package top.bulgat.migration.admin.domain.model;

import java.time.LocalDateTime;
import lombok.Getter;
import top.bulgat.migration.config.common.model.enums.GrayscaleRuleType;

/**
 * 灰度规则实体。
 */
@Getter
public class GrayscaleRule {

    private final String ruleId;
    private final String migrationKey;
    private GrayscaleRuleType ruleType;
    private String ruleValue;
    private boolean enable;
    private final LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer weight;

    /**
     * 使用当前时间创建规则。
     */
    public GrayscaleRule(
            String ruleId,
            String migrationKey,
            GrayscaleRuleType ruleType,
            String ruleValue,
            boolean enable,
            Integer weight) {
        this(ruleId, migrationKey, ruleType, ruleValue, enable, weight, LocalDateTime.now(), LocalDateTime.now());
    }

    /**
     * 使用指定时间创建规则。
     */
    public GrayscaleRule(
            String ruleId,
            String migrationKey,
            GrayscaleRuleType ruleType,
            String ruleValue,
            boolean enable,
            Integer weight,
            LocalDateTime createTime,
            LocalDateTime updateTime) {
        this.ruleId = ruleId;
        this.migrationKey = migrationKey;
        this.ruleType = ruleType;
        this.ruleValue = ruleValue;
        this.enable = enable;
        this.weight = weight;
        this.createTime = createTime;
        this.updateTime = updateTime;
    }

    /**
     * 部分更新规则字段。
     */
    public void update(GrayscaleRuleType targetType, String targetValue, Boolean targetEnable, Integer targetWeight) {
        if (targetType != null) {
            this.ruleType = targetType;
        }
        if (targetValue != null) {
            this.ruleValue = targetValue;
        }
        if (targetEnable != null) {
            this.enable = targetEnable;
        }
        if (targetWeight != null) {
            this.weight = targetWeight;
        }
        this.updateTime = LocalDateTime.now();
    }

    /**
     * 切换规则启用状态。
     */
    public void updateEnable(boolean targetEnable) {
        this.enable = targetEnable;
        this.updateTime = LocalDateTime.now();
    }
}
