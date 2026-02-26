package top.bulgat.migration.admin.application.command;

/**
 * DeleteMigrationTaskCommand contains the migration task key to delete.
 */
public record DeleteMigrationTaskCommand(String migrationKey) {
}
