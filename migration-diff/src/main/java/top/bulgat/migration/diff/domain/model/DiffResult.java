package top.bulgat.migration.diff.domain.model;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Diff execution result module.
 */
@Getter
@AllArgsConstructor
public class DiffResult {

    private final boolean hasDiff;
    private final List<DiffItem> diffItems;
    private final long costTimeMs;

    /**
     * Keep backward compatible boolean accessor used by existing services module.
     */
    public boolean hasDiff() {
        return hasDiff;
    }
}
