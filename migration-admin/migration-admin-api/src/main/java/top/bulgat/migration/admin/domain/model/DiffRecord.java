package top.bulgat.migration.admin.domain.model;

import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Diff record aggregate for admin read side module.
 */
@Getter
@AllArgsConstructor
public class DiffRecord {

    private final long id;
    private final String migrationKey;
    private final String traceId;
    private final String oldResponse;
    private final String newResponse;
    private final List<DiffItem> diffResults;
    private final boolean hasDiff;
    private final String diffType;
    private final String grayscaleParam;
    private final Integer oldCostTimeMs;
    private final Integer newCostTimeMs;
    private final Integer totalCostTimeMs;
    private final LocalDateTime createTime;
}
