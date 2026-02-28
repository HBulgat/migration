package top.bulgat.migration.diff.interfaces.assembler;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import top.bulgat.migration.diff.application.command.ExecuteDiffCommand;
import top.bulgat.migration.diff.domain.model.DiffResult;
import top.bulgat.migration.diff.interfaces.dto.DiffExecuteRequest;
import top.bulgat.migration.diff.interfaces.dto.DiffExecuteResponse;

/**
 * DiffCommandAssembler 用于转换DTO和领域模型。
 */
@Component
public class DiffCommandAssembler {

    /**
     * 执行 toCommand 业务逻辑。
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
                request.grayscaleParam());
    }

    /**
     * 执行 toResponse 业务逻辑。
     * @param result result object.
     * @return 返回结果。
     */
    public DiffExecuteResponse toResponse(DiffResult result) {
        List<DiffExecuteResponse.DiffItemResponse> items = result.getDiffItems().stream()
                .map(item -> new DiffExecuteResponse.DiffItemResponse(
                        item.getFieldPath(),
                        item.getOldValue(),
                        item.getNewValue(),
                        item.getDiffType().name()))
                .collect(Collectors.toList());
        return new DiffExecuteResponse(result.hasDiff(), items, result.getCostTimeMs());
    }
}
