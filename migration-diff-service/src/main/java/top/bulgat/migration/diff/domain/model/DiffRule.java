package top.bulgat.migration.diff.domain.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Diff rule module.
 */
@Getter
@AllArgsConstructor
public class DiffRule {

    private final String migrationKey;
    private final DiffRuleType ruleType;
    private final String fieldPath;
    private final String ruleValue;
    private final boolean enable;
}
