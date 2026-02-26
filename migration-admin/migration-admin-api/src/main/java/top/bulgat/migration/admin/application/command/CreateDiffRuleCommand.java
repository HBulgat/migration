package top.bulgat.migration.admin.application.command;

public record CreateDiffRuleCommand(
        String migrationKey,
        String ruleType,
        String fieldPath,
        String ruleValue,
        boolean enable
) {}
