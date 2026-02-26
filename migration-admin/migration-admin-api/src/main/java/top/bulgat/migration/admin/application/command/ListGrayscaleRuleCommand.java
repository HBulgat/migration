package top.bulgat.migration.admin.application.command;

/**
 * ListGrayscaleRuleCommand contains filter and pagination arguments for rule listing.
 */
public record ListGrayscaleRuleCommand(
        String migrationKey,
        int page,
        int pageSize) {
}
