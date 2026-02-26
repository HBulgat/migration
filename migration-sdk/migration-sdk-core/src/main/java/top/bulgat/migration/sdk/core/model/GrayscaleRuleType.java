package top.bulgat.migration.sdk.core.model;

/**
 * 灰度规则类型枚举。
 */
public enum GrayscaleRuleType {
    PERCENTAGE,
    BLACKLIST,
    WHITELIST,
    EXPRESSION;

    /**
     * 根据字符串解析规则类型。
     *
     * @param value 规则类型字符串
     * @return 规则类型枚举
     */
    public static GrayscaleRuleType fromValue(String value) {
        return GrayscaleRuleType.valueOf(value.toUpperCase());
    }
}
