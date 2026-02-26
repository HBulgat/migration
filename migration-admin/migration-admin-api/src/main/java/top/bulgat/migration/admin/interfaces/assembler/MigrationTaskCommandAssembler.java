package top.bulgat.migration.admin.interfaces.assembler;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import top.bulgat.migration.admin.application.command.CreateMigrationTaskCommand;
import top.bulgat.migration.admin.application.command.DeleteMigrationTaskCommand;
import top.bulgat.migration.admin.application.command.ListMigrationTaskCommand;
import top.bulgat.migration.admin.application.command.QueryMigrationTaskCommand;
import top.bulgat.migration.admin.application.command.UpdateMigrationTaskCommand;
import top.bulgat.migration.admin.application.command.UpdateMigrationTaskStatusCommand;
import top.bulgat.migration.admin.domain.model.MigrationTask;
import top.bulgat.migration.admin.interfaces.dto.CreateMigrationTaskRequest;
import top.bulgat.migration.admin.interfaces.dto.DeleteMigrationTaskRequest;
import top.bulgat.migration.admin.interfaces.dto.MigrationTaskResponse;
import top.bulgat.migration.admin.interfaces.dto.QueryMigrationTaskRequest;
import top.bulgat.migration.admin.interfaces.dto.UpdateMigrationTaskRequest;
import top.bulgat.migration.admin.interfaces.dto.UpdateMigrationTaskStatusRequest;

/**
 * MigrationTaskCommandAssembler converts DTOs and domain models.
 */
@Component
public class MigrationTaskCommandAssembler {

    /**
     * Execute toCommand business logic.
     * @param request request payload.
     * @return result value.
     */
    public CreateMigrationTaskCommand toCommand(CreateMigrationTaskRequest request) {
        return new CreateMigrationTaskCommand(request.migrationKey(), request.status(), request.description());
    }



    /**
     * Execute toUpdateCommand business logic.
     * @param request request payload.
     * @return result value.
     */
    public UpdateMigrationTaskCommand toUpdateCommand(UpdateMigrationTaskRequest request) {
        return new UpdateMigrationTaskCommand(request.migrationKey(), request.status(), request.description());
    }

    /**
     * Execute toUpdateStatusCommand business logic.
     * @param request request payload.
     * @return result value.
     */
    public UpdateMigrationTaskStatusCommand toUpdateStatusCommand(UpdateMigrationTaskStatusRequest request) {
        return new UpdateMigrationTaskStatusCommand(request.migrationKey(), request.targetStatus());
    }

    /**
     * Execute toQueryCommand business logic.
     * @param request request payload.
     * @return result value.
     */
    public QueryMigrationTaskCommand toQueryCommand(QueryMigrationTaskRequest request) {
        return new QueryMigrationTaskCommand(request.migrationKey());
    }

    /**
     * Execute toDeleteCommand business logic.
     * @param request request payload.
     * @return result value.
     */
    public DeleteMigrationTaskCommand toDeleteCommand(DeleteMigrationTaskRequest request) {
        return new DeleteMigrationTaskCommand(request.migrationKey());
    }



    /**
     * Execute toListCommand business logic.
     * @param status status filter.
     * @param keyword keyword filter.
     * @param page page index.
     * @param pageSize page size.
     * @return result value.
     */
    public ListMigrationTaskCommand toListCommand(Integer status, String keyword, int page, int pageSize) {
        return new ListMigrationTaskCommand(status, keyword, page, pageSize);
    }

    /**
     * Execute toResponse business logic.
     * @param task task entity.
     * @return result value.
     */
    public MigrationTaskResponse toResponse(MigrationTask task) {
        return new MigrationTaskResponse(
                task.getMigrationKey(),
                task.getStatus().getCode(),
                task.getDescription(),
                task.getCreateTime(),
                task.getUpdateTime());
    }

    /**
     * Execute toResponseList business logic.
     * @param tasks method parameter.
     * @return result value.
     */
    public List<MigrationTaskResponse> toResponseList(List<MigrationTask> tasks) {
        return tasks.stream().map(this::toResponse).collect(Collectors.toList());
    }
}
