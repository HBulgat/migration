package top.bulgat.migration.admin.domain.model;

/**
 * 灰度规则类型枚举。
 */
public enum GrayscaleRuleType {
    PERCENTAGE,
    BLACKLIST,
    WHITELIST,
    EXPRESSION;

    /**
     * 从文本解析规则类型。
     */
    public static GrayscaleRuleType fromValue(String value) {
        return GrayscaleRuleType.valueOf(value.toUpperCase());
    }
}
