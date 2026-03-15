package top.bulgat.migration.diff.interfaces.assembler;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import top.bulgat.migration.diff.application.command.ExecuteDiffCommand;
import top.bulgat.migration.diff.domain.model.DiffResult;
import top.bulgat.migration.diff.interfaces.dto.DiffExecuteRequest;
import top.bulgat.migration.diff.interfaces.dto.DiffExecuteResponse;

/**
 * 用于转换 DTO 与领域模型。
 */
@Component
public class DiffCommandAssembler {

    /**
     * 执行 toCommand 业务逻辑。
     * 
     * @param request 请求参数。
     * @return 返回结果。
     */
    public ExecuteDiffCommand toCommand(DiffExecuteRequest request) {
        return new ExecuteDiffCommand(
                request.migrationKey(),
                request.traceId(),
                request.oldJson(),
                request.newJson(),
                request.oldCostTimeMs(),
                request.newCostTimeMs(),
                request.grayscaleParam(),
                request.oldSuccess(),
                request.newSuccess(),
                request.oldErrorMessage(),
                request.newErrorMessage(),
                request.oldRequestParams(),
                request.newRequestParams(),
                request.migrationTaskStatus(),
                request.grayscaleRules(),
                request.grayscaleHit(),
                request.fallbackTriggered());
    }

    /**
     * 执行 toResponse 业务逻辑。
     * 
     * @param result 结果对象。
     * @return 返回结果。
     */
    public DiffExecuteResponse toResponse(DiffResult result) {
        List<DiffExecuteResponse.DiffResultItem> items = result.getDiffItems().stream()
                .map(item -> new DiffExecuteResponse.DiffResultItem(
                        item.fieldPath(),
                        item.oldValue(),
                        item.newValue(),
                        item.diffType().name()))
                .collect(Collectors.toList());
        return new DiffExecuteResponse(result.hasDiff(), items, result.getCostTimeMs());
    }
}
