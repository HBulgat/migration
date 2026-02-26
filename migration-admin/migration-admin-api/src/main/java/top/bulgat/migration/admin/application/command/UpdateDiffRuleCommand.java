package top.bulgat.migration.admin.application.command;

public record UpdateDiffRuleCommand(
        String migrationKey,
        String ruleId,
        String ruleType,
        String fieldPath,
        String ruleValue,
        Boolean enable
) {}
