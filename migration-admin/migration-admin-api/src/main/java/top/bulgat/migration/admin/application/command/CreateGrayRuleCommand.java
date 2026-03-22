package top.bulgat.migration.admin.application.command;

/**
 * 创建灰度规则命令，包含创建参数。
 */
public record CreateGrayRuleCommand(
        String migrationKey,
        String ruleType,
        String ruleValue,
        boolean enable) {
}

