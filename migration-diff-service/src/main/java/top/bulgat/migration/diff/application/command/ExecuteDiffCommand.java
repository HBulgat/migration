package top.bulgat.migration.diff.application.command;

/**
 * Diff 执行应用命令。
 */
public record ExecuteDiffCommand(
                String migrationKey,
                String traceId,
                String oldJson,
                String newJson,
                Integer oldCostTimeMs,
                Integer newCostTimeMs,
                String grayscaleParam,
                Boolean oldSuccess,
                Boolean newSuccess,
                String oldErrorMessage,
                String newErrorMessage,
                String oldRequestParams,
                String newRequestParams,
                Integer MigrationTaskStatus,
                String grayscaleRules,
                Boolean grayscaleHit,
                Boolean fallbackTriggered) {
}
