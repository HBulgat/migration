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
 * 用于转换 DTO 与领域模型。
 */
@Component
public class MigrationTaskCommandAssembler {

    /**
     * 执行 toCommand 业务逻辑。
     * @param request 请求参数。
     * @return 返回结果。
     */
    public CreateMigrationTaskCommand toCommand(CreateMigrationTaskRequest request) {
        return new CreateMigrationTaskCommand(request.migrationKey(), request.status(), request.description());
    }



    /**
     * 执行 toUpdateCommand 业务逻辑。
     * @param request 请求参数。
     * @return 返回结果。
     */
    public UpdateMigrationTaskCommand toUpdateCommand(UpdateMigrationTaskRequest request) {
        return new UpdateMigrationTaskCommand(request.migrationKey(), request.status(), request.description());
    }

    /**
     * 执行 toUpdateStatusCommand 业务逻辑。
     * @param request 请求参数。
     * @return 返回结果。
     */
    public UpdateMigrationTaskStatusCommand toUpdateStatusCommand(UpdateMigrationTaskStatusRequest request) {
        return new UpdateMigrationTaskStatusCommand(request.migrationKey(), request.targetStatus());
    }

    /**
     * 执行 toQueryCommand 业务逻辑。
     * @param request 请求参数。
     * @return 返回结果。
     */
    public QueryMigrationTaskCommand toQueryCommand(QueryMigrationTaskRequest request) {
        return new QueryMigrationTaskCommand(request.migrationKey());
    }

    /**
     * 执行 toDeleteCommand 业务逻辑。
     * @param request 请求参数。
     * @return 返回结果。
     */
    public DeleteMigrationTaskCommand toDeleteCommand(DeleteMigrationTaskRequest request) {
        return new DeleteMigrationTaskCommand(request.migrationKey());
    }



    /**
     * 执行 toListCommand 业务逻辑。
     * @param status 状态过滤条件。
     * @param keyword 关键字过滤条件。
     * @param page 页码。
     * @param pageSize 每页大小。
     * @return 返回结果。
     */
    public ListMigrationTaskCommand toListCommand(Integer status, String keyword, int page, int pageSize) {
        return new ListMigrationTaskCommand(status, keyword, page, pageSize);
    }

    /**
     * 执行 toResponse 业务逻辑。
     * @param task 任务实体。
     * @return 返回结果。
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
     * 执行 toResponseList 业务逻辑。
     * @param tasks 方法参数。
     * @return 返回结果。
     */
    public List<MigrationTaskResponse> toResponseList(List<MigrationTask> tasks) {
        return tasks.stream().map(this::toResponse).collect(Collectors.toList());
    }
}
