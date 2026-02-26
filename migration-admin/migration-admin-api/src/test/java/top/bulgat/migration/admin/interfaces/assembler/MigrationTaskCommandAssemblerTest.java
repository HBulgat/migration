package top.bulgat.migration.admin.interfaces.assembler;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import top.bulgat.migration.admin.application.command.CreateMigrationTaskCommand;
import top.bulgat.migration.admin.application.command.DeleteMigrationTaskCommand;
import top.bulgat.migration.admin.application.command.ListMigrationTaskCommand;
import top.bulgat.migration.admin.application.command.QueryMigrationTaskCommand;
import top.bulgat.migration.admin.application.command.UpdateMigrationTaskCommand;
import top.bulgat.migration.admin.application.command.UpdateMigrationTaskStatusCommand;
import top.bulgat.migration.admin.domain.model.MigrationStatus;
import top.bulgat.migration.admin.domain.model.MigrationTask;
import top.bulgat.migration.admin.interfaces.dto.CreateMigrationTaskRequest;
import top.bulgat.migration.admin.interfaces.dto.DeleteMigrationTaskRequest;
import top.bulgat.migration.admin.interfaces.dto.MigrationTaskResponse;
import top.bulgat.migration.admin.interfaces.dto.QueryMigrationTaskRequest;
import top.bulgat.migration.admin.interfaces.dto.UpdateMigrationTaskRequest;
import top.bulgat.migration.admin.interfaces.dto.UpdateMigrationTaskStatusRequest;

class MigrationTaskCommandAssemblerTest {

    private final MigrationTaskCommandAssembler assembler = new MigrationTaskCommandAssembler();

    @Test
    void toCommand_shouldMapRequestFields() {
        CreateMigrationTaskRequest request = new CreateMigrationTaskRequest("user.query", 2, "desc");

        CreateMigrationTaskCommand command = assembler.toCommand(request);

        assertEquals("user.query", command.migrationKey());
        assertEquals(2, command.status());
        assertEquals("desc", command.description());
    }

    @Test
    void toPatchCommands_shouldMapRequestFields() {
        UpdateMigrationTaskRequest updateRequest = new UpdateMigrationTaskRequest("user.query", 3, "desc-2");
        UpdateMigrationTaskStatusRequest statusRequest = new UpdateMigrationTaskStatusRequest("user.query", 4);
        QueryMigrationTaskRequest queryRequest = new QueryMigrationTaskRequest("user.query");
        DeleteMigrationTaskRequest deleteRequest = new DeleteMigrationTaskRequest("user.query");

        UpdateMigrationTaskCommand updateCommand = assembler.toUpdateCommand(updateRequest);
        UpdateMigrationTaskStatusCommand updateStatusCommand = assembler.toUpdateStatusCommand(statusRequest);
        QueryMigrationTaskCommand queryCommand = assembler.toQueryCommand(queryRequest);
        DeleteMigrationTaskCommand deleteCommand = assembler.toDeleteCommand(deleteRequest);
        ListMigrationTaskCommand listCommand = assembler.toListCommand(3, "user", 2, 20);

        assertEquals("user.query", updateCommand.migrationKey());
        assertEquals(3, updateCommand.status());
        assertEquals("desc-2", updateCommand.description());
        assertEquals("user.query", updateStatusCommand.migrationKey());
        assertEquals(4, updateStatusCommand.targetStatus());
        assertEquals("user.query", queryCommand.migrationKey());
        assertEquals("user.query", deleteCommand.migrationKey());
        assertEquals(3, listCommand.status());
        assertEquals("user", listCommand.keyword());
        assertEquals(2, listCommand.page());
        assertEquals(20, listCommand.pageSize());
    }


    @Test
    void toResponseAndList_shouldMapDomainFields() {
        LocalDateTime now = LocalDateTime.of(2026, 2, 23, 21, 0);
        MigrationTask task = new MigrationTask("user.query", MigrationStatus.VALIDATION_GRAY, "desc", now, now);

        MigrationTaskResponse response = assembler.toResponse(task);
        List<MigrationTaskResponse> responseList = assembler.toResponseList(List.of(task));

        assertEquals("user.query", response.migrationKey());
        assertEquals(2, response.status());
        assertEquals(now, response.createTime());
        assertEquals(1, responseList.size());
        assertEquals("user.query", responseList.get(0).migrationKey());
    }
}
