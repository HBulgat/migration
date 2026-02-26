package top.bulgat.migration.sdk.core.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 迁移状态枚举。
 */
@Getter
@AllArgsConstructor
public enum MigrationStatus {

    OLD(1, "单旧"),
    VALIDATION_GRAY(2, "验证-灰度"),
    VALIDATION_ALL(3, "验证-全开"),
    GO_LIVE_GRAY(4, "上线-灰度"),
    GO_LIVE_ALL(5, "上线-全开"),
    DECOMMISSIONING_GRAY(6, "停用-灰度"),
    DECOMMISSIONING_ALL(7, "停用-全开");

    private final int code;
    private final String desc;

    /**
     * 根据状态码解析迁移状态。
     *
     * @param code 状态码
     * @return 迁移状态
     */
    public static MigrationStatus fromCode(int code) {
        for (MigrationStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        throw new IllegalArgumentException("unknown migration status: " + code);
    }
}
