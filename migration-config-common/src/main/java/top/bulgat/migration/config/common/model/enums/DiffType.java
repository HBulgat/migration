package top.bulgat.migration.config.common.model.enums;

import top.bulgat.common.base.util.StringUtils;

/**
 * DiffType 枚举定义。
 */
public enum DiffType {
    ADD,
    REMOVE,
    MODIFY
    ;
    public static DiffType fromValue(String value) {
        if (StringUtils.isBlank(value)) {
            throw new IllegalArgumentException("diff type cannot be null");
        }
        return valueOf(value.toUpperCase());
    }
}

