package top.bulgat.migration.admin.application.command;

/**
 * 删除灰度规则命令，包含规则标识。
 */
public record DeleteGrayscaleRuleCommand(
        String migrationKey,
        String ruleId) {
}
