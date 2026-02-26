package top.bulgat.migration.admin.application.command;

/**
 * CreateGrayscaleRuleCommand contains payload for creating a grayscale rule.
 */
public record CreateGrayscaleRuleCommand(
        String migrationKey,
        String ruleType,
        String ruleValue,
        boolean enable) {
}
