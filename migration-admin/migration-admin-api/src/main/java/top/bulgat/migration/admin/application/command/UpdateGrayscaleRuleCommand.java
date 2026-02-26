package top.bulgat.migration.admin.application.command;

/**
 * UpdateGrayscaleRuleCommand contains patch fields for updating a grayscale rule.
 */
public record UpdateGrayscaleRuleCommand(
        String migrationKey,
        String ruleId,
        String ruleType,
        String ruleValue,
        Boolean enable) {
}
