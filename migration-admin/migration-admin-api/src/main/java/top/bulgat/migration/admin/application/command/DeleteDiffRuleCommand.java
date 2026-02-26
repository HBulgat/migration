package top.bulgat.migration.admin.application.command;

public record DeleteDiffRuleCommand(
        String migrationKey,
        String ruleId
) {}
