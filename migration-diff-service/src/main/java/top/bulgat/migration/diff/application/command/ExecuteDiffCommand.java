package top.bulgat.migration.diff.application.command;

import top.bulgat.migration.diff.domain.model.DiffRequest;

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
                Integer migrationTaskStatus,
                String grayscaleRules,
                Boolean grayscaleHit,
                Boolean fallbackTriggered) {

    /**
     * 将应用层命令统一转换为领域层请求实体。
     *
     * @return DiffRequest 分配给领域层处理的实体对象。
     */
    public DiffRequest toDiffRequest() {
        return new DiffRequest(
                migrationKey, traceId, oldJson, newJson, oldCostTimeMs, newCostTimeMs,
                grayscaleParam, oldSuccess, newSuccess, oldErrorMessage, newErrorMessage,
                oldRequestParams, newRequestParams, migrationTaskStatus, grayscaleRules,
                grayscaleHit, fallbackTriggered
        );
    }
}
