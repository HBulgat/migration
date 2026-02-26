package top.bulgat.migration.admin.application.command;

/**
 * ListMigrationTaskCommand contains filter and pagination arguments for task listing.
 */
public record ListMigrationTaskCommand(
        Integer status,
        String keyword,
        int page,
        int pageSize) {
}
