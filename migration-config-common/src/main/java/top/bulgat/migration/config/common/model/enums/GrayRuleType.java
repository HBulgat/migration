package top.bulgat.migration.config.common.model.enums;

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
        if (value == null) {
            throw new IllegalArgumentException("gray rule type cannot be null");
        }
        return GrayRuleType.valueOf(value.toUpperCase());
    }
}
