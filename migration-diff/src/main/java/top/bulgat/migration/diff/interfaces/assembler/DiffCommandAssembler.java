package top.bulgat.migration.diff.interfaces.assembler;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import top.bulgat.migration.diff.application.command.ExecuteDiffCommand;
import top.bulgat.migration.diff.domain.model.DiffResult;
import top.bulgat.migration.diff.interfaces.dto.DiffExecuteRequest;
import top.bulgat.migration.diff.interfaces.dto.DiffExecuteResponse;

/**
 * DiffCommandAssembler converts DTOs and domain models.
 */
@Component
public class DiffCommandAssembler {

    /**
     * Execute toCommand business logic.
     * @param request request payload.
     * @return result value.
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
     * Execute toResponse business logic.
     * @param result result object.
     * @return result value.
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
