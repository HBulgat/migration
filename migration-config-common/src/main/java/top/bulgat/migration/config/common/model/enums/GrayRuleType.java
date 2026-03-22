package top.bulgat.migration.config.common.model.enums;

import top.bulgat.common.base.util.StringUtils;

/**
 * 灰度规则类型枚举。
 */
public enum GrayRuleType {
    PERCENTAGE,
    BLACKLIST,
    WHITELIST,
    EXPRESSION;

    /**
     * 从文本解析规则类型。
     */
    public static GrayRuleType fromValue(String value) {
        if (StringUtils.isBlank(value)) {
            throw new IllegalArgumentException("gray rule type cannot be null");
        }
        return GrayRuleType.valueOf(value.toUpperCase());
    }
}
