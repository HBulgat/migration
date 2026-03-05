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
    private final Boolean oldSuccess;
    private final Boolean newSuccess;
    private final String oldErrorMessage;
    private final String newErrorMessage;
    private final String oldRequestParams;
    private final String newRequestParams;
    private final Integer migrationStatus;
    private final String grayscaleRules;
    private final Boolean grayscaleHit;
    private final Boolean fallbackTriggered;
}
