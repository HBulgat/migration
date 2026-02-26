package top.bulgat.migration.diff.application.command;

/**
 * ExecuteDiffCommand is an application command.
 */
public record ExecuteDiffCommand(
        String migrationKey,
        String traceId,
        String oldJson,
        String newJson,
        Integer oldCostTimeMs,
        Integer newCostTimeMs,
        String grayscaleParam) {
}
