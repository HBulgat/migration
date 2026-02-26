package top.bulgat.migration.admin.domain.service;

import org.springframework.stereotype.Component;
import top.bulgat.migration.admin.domain.model.GrayscaleRule;
import top.bulgat.migration.admin.domain.model.GrayscaleRuleType;

/**
 * 灰度规则领域服务。
 * 负责灰度规则类型和值的领域校验。
 */
@Component
public class GrayscaleRuleDomainService {

    /**
     * 校验灰度规则实体。
     *
     * @param rule 规则实体
     */
    public void validateRule(GrayscaleRule rule) {
        validateRuleValue(rule.getRuleType(), rule.getRuleValue());
    }

    /**
     * 按规则类型校验规则值格式。
     *
     * @param type 规则类型
     * @param value 规则值
     */
    public void validateRuleValue(GrayscaleRuleType type, String value) {
        if (type == null) {
            throw new IllegalArgumentException("rule_type is required");
        }
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("rule_value is required");
        }
        switch (type) {
            case PERCENTAGE -> validatePercentage(value);
            case BLACKLIST, WHITELIST -> validateJsonArray(value);
            case EXPRESSION -> {
                // EXPRESSION 目前只做非空校验，具体表达式语法由调用方约束。
            }
        }
    }

    private void validatePercentage(String value) {
        int percentage;
        try {
            percentage = Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("rule_value must be integer for PERCENTAGE");
        }
        if (percentage < 0 || percentage > 100) {
            throw new IllegalArgumentException("rule_value out of range [0,100]");
        }
    }

    private void validateJsonArray(String value) {
        String trimmed = value.trim();
        if (!trimmed.startsWith("[") || !trimmed.endsWith("]")) {
            throw new IllegalArgumentException("rule_value must be JSON array for BLACKLIST/WHITELIST");
        }
    }
}

