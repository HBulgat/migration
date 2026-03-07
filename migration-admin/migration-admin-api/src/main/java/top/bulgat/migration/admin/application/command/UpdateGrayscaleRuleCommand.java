package top.bulgat.migration.admin.application.command;

/**
 * 更新灰度规则命令，包含可变更字段。
 */
public record UpdateGrayscaleRuleCommand(
        String migrationKey,
        String ruleId,
        String ruleType,
        String ruleValue,
        Boolean enable) {
}
