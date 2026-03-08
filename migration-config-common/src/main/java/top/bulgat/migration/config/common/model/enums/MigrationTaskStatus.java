package top.bulgat.migration.config.common.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 迁移生命周期状态枚举。
 */
@Getter
@AllArgsConstructor
public enum MigrationTaskStatus {
    OLD(1, "OLD"),
    VALIDATION_GRAY(2, "VALIDATION_GRAY"),
    VALIDATION_ALL(3, "VALIDATION_ALL"),
    GO_LIVE_GRAY(4, "GO_LIVE_GRAY"),
    GO_LIVE_ALL(5, "GO_LIVE_ALL"),
    DECOMMISSIONING_GRAY(6, "DECOMMISSIONING_GRAY"),
    DECOMMISSIONING_ALL(7, "DECOMMISSIONING_ALL");

    private final int code;
    private final String desc;

    /**
     * 根据数值编码解析状态。
     */
    public static MigrationTaskStatus fromCode(int code) {
        for (MigrationTaskStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown migration status: " + code);
    }

    /**
     * 校验当前状态是否允许切换到目标状态。
     */
    public boolean canSwitchTo(MigrationTaskStatus target) {
        if (target == this) {
            return true;
        }
        if (target.code < this.code) {
            return true;
        }
        return target.code == this.code + 1;
    }
}
