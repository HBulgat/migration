package top.bulgat.migration.diff.domain.model;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Diff 执行结果模型。
 */
@Getter
@AllArgsConstructor
public class DiffResult {

    private final boolean hasDiff;
    private final List<DiffItem> diffItems;
    private final long costTimeMs;

    /**
     * 保留现有服务使用的布尔访问方法，以兼容旧调用方。
     */
    public boolean hasDiff() {
        return hasDiff;
    }
}
