package top.bulgat.migration.admin.application.command;

/**
 * UpdateGrayscaleRuleEnableCommand contains payload for toggling grayscale rule enable status.
 */
public record UpdateGrayscaleRuleEnableCommand(
        String migrationKey,
        String ruleId,
        boolean enable) {
}
