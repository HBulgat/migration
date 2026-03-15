package top.bulgat.migration.diff.domain.model;


/**
 * Diff 规则模型。
 */
public record DiffRule(String migrationKey,
                       DiffRuleType ruleType,
                       String fieldPath,
                       String ruleValue,
                       boolean enable,
                       Integer weight) {
}

