package top.bulgat.migration.admin.application.command;

/**
 * 灰度规则启停命令，包含启停参数。
 */
public record UpdateGrayRuleEnableCommand(
        String migrationKey,
        String ruleId,
        boolean enable) {
}
