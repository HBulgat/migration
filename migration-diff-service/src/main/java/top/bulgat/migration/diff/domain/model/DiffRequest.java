package top.bulgat.migration.diff.domain.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Diff execution request in domain layer module.
 */
@Getter
@AllArgsConstructor
public class DiffRequest {

    private final String migrationKey;
    private final String traceId;
    private final String oldJson;
    private final String newJson;
    private final Integer oldCostTimeMs;
    private final Integer newCostTimeMs;
    private final String grayscaleParam;
}
