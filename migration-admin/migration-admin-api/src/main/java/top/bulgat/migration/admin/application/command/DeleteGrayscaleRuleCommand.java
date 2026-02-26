package top.bulgat.migration.admin.application.command;

/**
 * DeleteGrayscaleRuleCommand contains identifiers for deleting a grayscale rule.
 */
public record DeleteGrayscaleRuleCommand(
        String migrationKey,
        String ruleId) {
}
