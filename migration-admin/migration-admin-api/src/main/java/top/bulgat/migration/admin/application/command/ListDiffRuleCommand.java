package top.bulgat.migration.admin.application.command;

public record ListDiffRuleCommand(
        String migrationKey,
        int page,
        int pageSize
) {}
