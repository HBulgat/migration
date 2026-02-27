package top.bulgat.migration.diff.domain.model;

/**
 * Diff规则类型枚举。
 */
public enum DiffRuleType {
    IGNORE,
    TOLERANCE,
    SCRIPT,
    SORT;

    /**
     * 根据字符串解析规则类型。
     *
     * @param value 规则类型字符串
     * @return 对应规则类型
     */
    public static DiffRuleType fromValue(String value) {
        return DiffRuleType.valueOf(value.toUpperCase());
    }
}
