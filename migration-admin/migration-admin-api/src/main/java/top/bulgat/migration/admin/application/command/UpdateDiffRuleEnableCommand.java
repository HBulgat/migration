package top.bulgat.migration.admin.application.command;

public record UpdateDiffRuleEnableCommand(
        String migrationKey,
        String ruleId,
        boolean enable
) {}
