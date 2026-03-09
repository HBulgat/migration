package top.bulgat.migration.config.common.model.enums;

/**
 * DiffType 枚举定义。
 */
public enum DiffType {
    ADD,
    REMOVE,
    MODIFY
    ;
    public static DiffType fromValue(String value) {
        if (value == null) {
            throw new IllegalArgumentException("diff type cannot be null");
        }
        return valueOf(value.toUpperCase());
    }
}

