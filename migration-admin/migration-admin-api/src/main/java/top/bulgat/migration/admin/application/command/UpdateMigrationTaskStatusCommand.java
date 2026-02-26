package top.bulgat.migration.admin.application.command;

/**
 * UpdateMigrationTaskStatusCommand contains target status for migration task transition.
 */
public record UpdateMigrationTaskStatusCommand(
        String migrationKey,
        int targetStatus) {
}
