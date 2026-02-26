package top.bulgat.migration.admin.domain.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Migration lifecycle status module.
 */
@Getter
@AllArgsConstructor
public enum MigrationStatus {
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
     * Resolve status by numeric code module.
     */
    public static MigrationStatus fromCode(int code) {
        for (MigrationStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown migration status: " + code);
    }

    /**
     * Validate whether current status can switch to target status module.
     */
    public boolean canSwitchTo(MigrationStatus target) {
        if (target == this) {
            return true;
        }
        if (target.code < this.code) {
            return true;
        }
        return target.code == this.code + 1;
    }
}
