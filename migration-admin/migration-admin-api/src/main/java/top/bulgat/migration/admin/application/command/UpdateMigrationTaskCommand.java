package top.bulgat.migration.admin.application.command;

/**
 * UpdateMigrationTaskCommand contains patch fields for updating a migration task.
 */
public record UpdateMigrationTaskCommand(
        String migrationKey,
        Integer status,
        String description) {
}
