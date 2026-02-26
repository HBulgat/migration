package top.bulgat.migration.admin.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import top.bulgat.common.exception.BizException;
import top.bulgat.common.exception.ErrorCode;
import top.bulgat.migration.admin.application.command.CreateMigrationTaskCommand;
import top.bulgat.migration.admin.application.command.DeleteMigrationTaskCommand;
import top.bulgat.migration.admin.application.command.ListMigrationTaskCommand;
import top.bulgat.migration.admin.application.command.QueryMigrationTaskCommand;
import top.bulgat.migration.admin.application.command.UpdateMigrationTaskCommand;
import top.bulgat.migration.admin.application.command.UpdateMigrationTaskStatusCommand;
import top.bulgat.migration.admin.domain.model.GrayscaleRule;
import top.bulgat.migration.admin.domain.model.GrayscaleRuleType;
import top.bulgat.migration.admin.domain.model.MigrationStatus;
import top.bulgat.migration.admin.domain.model.MigrationTask;
import top.bulgat.migration.admin.domain.service.MigrationTaskDomainService;
import top.bulgat.migration.admin.infrastructure.repository.InMemoryGrayscaleRuleRepository;
import top.bulgat.migration.admin.infrastructure.repository.InMemoryMigrationTaskRepository;

class MigrationTaskApplicationServiceTest {

    private final InMemoryMigrationTaskRepository taskRepository = new InMemoryMigrationTaskRepository();
    private final InMemoryGrayscaleRuleRepository grayscaleRuleRepository = new InMemoryGrayscaleRuleRepository();
    private final MigrationTaskApplicationService service = new MigrationTaskApplicationService(
            taskRepository,
            grayscaleRuleRepository,
            new top.bulgat.migration.admin.infrastructure.repository.InMemoryDiffRuleRepository(),
            new MigrationTaskDomainService());

    @Test
    void createAndList_shouldReturnPagedTasks() {
        service.createMigrationTask(new CreateMigrationTaskCommand("user.query", 1, "query old API"));
        service.createMigrationTask(new CreateMigrationTaskCommand("user.update", 2, "update validate"));

        List<MigrationTask> firstPage = service.list(new ListMigrationTaskCommand(null, "user", 1, 10));

        assertEquals(2, firstPage.size());
        assertEquals(2L, service.count(new ListMigrationTaskCommand(null, "user", 1, 10)));
        assertTrue(service.existsByMigrationKey("user.query"));
    }

    @Test
    void updateStatus_shouldRejectIllegalForwardTransition() {
        service.createMigrationTask(new CreateMigrationTaskCommand("order.sync", 1, "order init"));

        BizException error = assertThrows(
                BizException.class,
                () -> service.updateStatus(new UpdateMigrationTaskStatusCommand("order.sync", 3)));

        assertEquals(ErrorCode.PARAM_ERROR.getCode(), error.getCode());
        assertTrue(error.getMessage().contains("invalid status switch"));
    }

    @Test
    void deleteByMigrationKey_shouldDeleteRelatedGrayscaleRules() {
        service.createMigrationTask(new CreateMigrationTaskCommand("stock.sync", 1, "stock init"));
        grayscaleRuleRepository.save(new GrayscaleRule(
                "rule-1",
                "stock.sync",
                GrayscaleRuleType.WHITELIST,
                "[\"u1\"]",
                true));

        service.deleteByMigrationKey(new DeleteMigrationTaskCommand("stock.sync"));

        assertFalse(service.existsByMigrationKey("stock.sync"));
        assertEquals(0, grayscaleRuleRepository.findByMigrationKey("stock.sync").size());
    }

    @Test
    void updateTask_shouldApplyDescriptionAndStatusChanges() {
        service.createMigrationTask(new CreateMigrationTaskCommand("pay.sync", 1, "init"));

        MigrationTask updated = service.updateTask(new UpdateMigrationTaskCommand(
                "pay.sync", MigrationStatus.VALIDATION_GRAY.getCode(), "step-2"));

        assertEquals(MigrationStatus.VALIDATION_GRAY, updated.getStatus());
        assertEquals("step-2", updated.getDescription());
    }

    @Test
    void list_withCommand_shouldApplyFiltersAndPaging() {
        service.createMigrationTask(new CreateMigrationTaskCommand("cmd.list.1", 1, "a"));
        service.createMigrationTask(new CreateMigrationTaskCommand("cmd.list.2", 2, "b"));

        List<MigrationTask> page = service.list(new ListMigrationTaskCommand(1, "cmd.list", 1, 10));
        long total = service.count(new ListMigrationTaskCommand(1, "cmd.list", 1, 10));

        assertEquals(1, page.size());
        assertEquals("cmd.list.1", page.get(0).getMigrationKey());
        assertEquals(1L, total);
    }

    @Test
    void updateStatus_withCommand_shouldApplyStatusChange() {
        service.createMigrationTask(new CreateMigrationTaskCommand("status.sync", 1, "init"));

        MigrationTask updated = service.updateStatus(new UpdateMigrationTaskStatusCommand("status.sync", 2));

        assertEquals(MigrationStatus.VALIDATION_GRAY, updated.getStatus());
    }

    @Test
    void list_shouldRejectInvalidStatusFilter() {
        BizException exception = assertThrows(
                BizException.class,
                () -> service.list(new ListMigrationTaskCommand(9, null, 1, 10)));
        assertEquals(ErrorCode.PARAM_ERROR.getCode(), exception.getCode());
    }

    @Test
    void list_shouldRejectInvalidPagination() {
        BizException exception = assertThrows(
                BizException.class,
                () -> service.list(new ListMigrationTaskCommand(null, null, 0, 10)));
        assertEquals(ErrorCode.PARAM_ERROR.getCode(), exception.getCode());
    }

    @Test
    void getByMigrationKey_shouldThrowNotFoundWhenTaskMissing() {
        BizException exception = assertThrows(
                BizException.class,
                () -> service.getByMigrationKey(new QueryMigrationTaskCommand("missing.task")));
        assertEquals(ErrorCode.NOT_FOUND.getCode(), exception.getCode());
    }

    @Test
    void getByMigrationKey_shouldRejectInvalidMigrationKeyFormat() {
        BizException tooLongException = assertThrows(
                BizException.class,
                () -> service.getByMigrationKey(new QueryMigrationTaskCommand("k".repeat(129))));
        BizException withWhitespaceException = assertThrows(
                BizException.class,
                () -> service.getByMigrationKey(new QueryMigrationTaskCommand("task key")));
        BizException withTabException = assertThrows(
                BizException.class,
                () -> service.getByMigrationKey(new QueryMigrationTaskCommand("task	key")));

        assertEquals(ErrorCode.PARAM_ERROR.getCode(), tooLongException.getCode());
        assertEquals(ErrorCode.PARAM_ERROR.getCode(), withWhitespaceException.getCode());
        assertEquals(ErrorCode.PARAM_ERROR.getCode(), withTabException.getCode());
    }

    @Test
    void updateTask_shouldRejectWhenNoFieldProvided() {
        service.createMigrationTask(new CreateMigrationTaskCommand("coupon.sync", 1, "init"));

        BizException exception = assertThrows(
                BizException.class,
                () -> service.updateTask(new UpdateMigrationTaskCommand("coupon.sync", null, null)));

        assertEquals(ErrorCode.PARAM_ERROR.getCode(), exception.getCode());
    }

    @Test
    void deleteByMigrationKey_shouldThrowWhenTaskMissing() {
        BizException exception = assertThrows(
                BizException.class,
                () -> service.deleteByMigrationKey(new DeleteMigrationTaskCommand("missing.task")));
        assertEquals(ErrorCode.NOT_FOUND.getCode(), exception.getCode());
    }


    @Test
    void commandApis_shouldThrowWhenCommandIsNull() {
        BizException createEx = assertThrows(BizException.class, () -> service.createMigrationTask(null));
        BizException updateEx = assertThrows(BizException.class, () -> service.updateTask(null));
        BizException queryEx = assertThrows(BizException.class, () -> service.getByMigrationKey(null));
        BizException deleteEx = assertThrows(BizException.class, () -> service.deleteByMigrationKey(null));
        BizException listEx = assertThrows(BizException.class, () -> service.list(null));
        BizException countEx = assertThrows(BizException.class, () -> service.count(null));
        BizException statusEx = assertThrows(BizException.class, () -> service.updateStatus(null));

        assertEquals(ErrorCode.PARAM_ERROR.getCode(), createEx.getCode());
        assertEquals(ErrorCode.PARAM_ERROR.getCode(), updateEx.getCode());
        assertEquals(ErrorCode.PARAM_ERROR.getCode(), queryEx.getCode());
        assertEquals(ErrorCode.PARAM_ERROR.getCode(), deleteEx.getCode());
        assertEquals(ErrorCode.PARAM_ERROR.getCode(), listEx.getCode());
        assertEquals(ErrorCode.PARAM_ERROR.getCode(), countEx.getCode());
        assertEquals(ErrorCode.PARAM_ERROR.getCode(), statusEx.getCode());
    }

}
