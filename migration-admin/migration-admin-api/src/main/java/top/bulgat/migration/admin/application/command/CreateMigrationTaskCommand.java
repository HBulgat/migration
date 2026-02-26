package top.bulgat.migration.admin.application.command;

/**
 * CreateMigrationTaskCommand is an application command.
 */
public record CreateMigrationTaskCommand(String migrationKey, int status, String description) {
}
