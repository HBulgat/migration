package top.bulgat.migration.config.common.model.enums;

/**
 * 差异规则类型。
 */
public enum DiffRuleType {
    IGNORE,
    TOLERANCE,
    SCRIPT,
    SORT;

    public static DiffRuleType fromValue(String value) {
        if (value == null) {
            throw new IllegalArgumentException("diff rule type cannot be null");
        }
        return valueOf(value.toUpperCase());
    }
}
