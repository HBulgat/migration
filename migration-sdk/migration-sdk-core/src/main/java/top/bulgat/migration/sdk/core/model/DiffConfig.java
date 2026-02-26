package top.bulgat.migration.sdk.core.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Diff rule configuration model.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiffConfig {

    /** Migration task key. */
    private String migrationKey;
    /** Rule type: IGNORE/TOLERANCE/SCRIPT/SORT. */
    private String ruleType;
    /** Field path (supports JSONPath). */
    private String fieldPath;
    /** Rule value. */
    private String ruleValue;
    /** Whether the rule is enabled. */
    private Boolean enable;
}
