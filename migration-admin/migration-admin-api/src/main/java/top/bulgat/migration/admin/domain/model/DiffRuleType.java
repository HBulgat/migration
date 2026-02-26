package top.bulgat.migration.admin.domain.model;

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
        return DiffRuleType.valueOf(value.toUpperCase());
    }
}
