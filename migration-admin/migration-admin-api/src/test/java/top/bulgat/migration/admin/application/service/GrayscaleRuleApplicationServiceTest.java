package top.bulgat.migration.admin.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import top.bulgat.common.exception.BizException;
import top.bulgat.common.exception.ErrorCode;
import top.bulgat.migration.admin.application.command.CreateGrayscaleRuleCommand;
import top.bulgat.migration.admin.application.command.CreateMigrationTaskCommand;
import top.bulgat.migration.admin.application.command.DeleteGrayscaleRuleCommand;
import top.bulgat.migration.admin.application.command.ListGrayscaleRuleCommand;
import top.bulgat.migration.admin.application.command.UpdateGrayscaleRuleCommand;
import top.bulgat.migration.admin.application.command.UpdateGrayscaleRuleEnableCommand;
import top.bulgat.migration.admin.domain.model.GrayscaleRule;
import top.bulgat.migration.admin.domain.model.GrayscaleRuleType;
import top.bulgat.migration.admin.domain.service.GrayscaleRuleDomainService;
import top.bulgat.migration.admin.domain.service.MigrationTaskDomainService;
import top.bulgat.migration.admin.infrastructure.repository.InMemoryGrayscaleRuleRepository;
import top.bulgat.migration.admin.infrastructure.repository.InMemoryMigrationTaskRepository;

class GrayscaleRuleApplicationServiceTest {

    private static final String MIGRATION_KEY = "user.profile";

    private GrayscaleRuleApplicationService service;

    @BeforeEach
    void setUp() {
        InMemoryMigrationTaskRepository taskRepository = new InMemoryMigrationTaskRepository();
        InMemoryGrayscaleRuleRepository ruleRepository = new InMemoryGrayscaleRuleRepository();
        MigrationTaskApplicationService taskService = new MigrationTaskApplicationService(
                taskRepository,
                ruleRepository,
                new top.bulgat.migration.admin.infrastructure.repository.InMemoryDiffRuleRepository(),
                new MigrationTaskDomainService());
        taskService.createMigrationTask(new CreateMigrationTaskCommand(MIGRATION_KEY, 1, "seed task"));
        service = new GrayscaleRuleApplicationService(
                taskService,
                ruleRepository,
                new GrayscaleRuleDomainService(),
                new MigrationTaskDomainService());
    }

    @Test
    void commandApis_shouldCreateListUpdateAndDelete() {
        GrayscaleRule created = service.create(new CreateGrayscaleRuleCommand(MIGRATION_KEY, "PERCENTAGE", "20", true));

        List<GrayscaleRule> page = service.list(new ListGrayscaleRuleCommand(MIGRATION_KEY, 1, 10));
        assertEquals(1, page.size());
        assertEquals(1L, service.count(new ListGrayscaleRuleCommand(MIGRATION_KEY, 1, 10)));

        service.update(new UpdateGrayscaleRuleCommand(MIGRATION_KEY, created.getRuleId(), null, "30", null));
        service.updateEnable(new UpdateGrayscaleRuleEnableCommand(MIGRATION_KEY, created.getRuleId(), false));

        GrayscaleRule updated = service.list(new ListGrayscaleRuleCommand(MIGRATION_KEY, 1, 10)).get(0);
        assertEquals("30", updated.getRuleValue());
        assertFalse(updated.isEnable());

        service.delete(new DeleteGrayscaleRuleCommand(MIGRATION_KEY, created.getRuleId()));
        assertTrue(service.list(new ListGrayscaleRuleCommand(MIGRATION_KEY, 1, 10)).isEmpty());
    }

    @Test
    void create_shouldThrowWhenTaskDoesNotExist() {
        MigrationTaskApplicationService emptyTaskService = new MigrationTaskApplicationService(
                new InMemoryMigrationTaskRepository(),
                new InMemoryGrayscaleRuleRepository(),
                new top.bulgat.migration.admin.infrastructure.repository.InMemoryDiffRuleRepository(),
                new MigrationTaskDomainService());
        GrayscaleRuleApplicationService target = new GrayscaleRuleApplicationService(
                emptyTaskService,
                new InMemoryGrayscaleRuleRepository(),
                new GrayscaleRuleDomainService(),
                new MigrationTaskDomainService());

        BizException exception = assertThrows(
                BizException.class,
                () -> target.create(new CreateGrayscaleRuleCommand("not-exist", "PERCENTAGE", "20", true)));
        assertEquals(ErrorCode.NOT_FOUND.getCode(), exception.getCode());
    }

    @Test
    void createAndList_shouldReturnPagedRules() {
        service.create(new CreateGrayscaleRuleCommand(MIGRATION_KEY, "PERCENTAGE", "10", true));
        service.create(new CreateGrayscaleRuleCommand(MIGRATION_KEY, "WHITELIST", "[\"u1\"]", false));

        List<GrayscaleRule> firstPage = service.list(new ListGrayscaleRuleCommand(MIGRATION_KEY, 1, 1));

        assertEquals(1, firstPage.size());
        assertEquals(2L, service.count(new ListGrayscaleRuleCommand(MIGRATION_KEY, 1, 10)));
    }

    @Test
    void update_shouldRejectInvalidPercentageRule() {
        GrayscaleRule created = service.create(new CreateGrayscaleRuleCommand(MIGRATION_KEY, "PERCENTAGE", "30", true));

        BizException exception = assertThrows(
                BizException.class,
                () -> service.update(new UpdateGrayscaleRuleCommand(
                        MIGRATION_KEY,
                        created.getRuleId(),
                        "PERCENTAGE",
                        "101",
                        true)));
        assertEquals(ErrorCode.PARAM_ERROR.getCode(), exception.getCode());
    }

    @Test
    void updateEnableAndDelete_shouldTakeEffect() {
        GrayscaleRule created = service.create(new CreateGrayscaleRuleCommand(MIGRATION_KEY, "WHITELIST", "[\"u2\"]", true));

        service.updateEnable(new UpdateGrayscaleRuleEnableCommand(MIGRATION_KEY, created.getRuleId(), false));
        List<GrayscaleRule> listed = service.list(new ListGrayscaleRuleCommand(MIGRATION_KEY, 1, 10));
        assertFalse(listed.get(0).isEnable());

        service.delete(new DeleteGrayscaleRuleCommand(MIGRATION_KEY, created.getRuleId()));
        assertTrue(service.list(new ListGrayscaleRuleCommand(MIGRATION_KEY, 1, 10)).isEmpty());
    }

    @Test
    void list_shouldRejectInvalidPagination() {
        BizException exception = assertThrows(
                BizException.class,
                () -> service.list(new ListGrayscaleRuleCommand(MIGRATION_KEY, 1, 0)));
        assertEquals(ErrorCode.PARAM_ERROR.getCode(), exception.getCode());
    }

    @Test
    void update_shouldThrowNotFoundWhenRuleMissing() {
        BizException exception = assertThrows(
                BizException.class,
                () -> service.update(new UpdateGrayscaleRuleCommand(
                        MIGRATION_KEY,
                        "missing-rule",
                        "WHITELIST",
                        "[\"u1\"]",
                        true)));
        assertEquals(ErrorCode.NOT_FOUND.getCode(), exception.getCode());
    }

    @Test
    void update_shouldThrowWhenNoFieldProvided() {
        GrayscaleRule created = service.create(new CreateGrayscaleRuleCommand(MIGRATION_KEY, "WHITELIST", "[\"u1\"]", true));

        BizException exception = assertThrows(
                BizException.class,
                () -> service.update(new UpdateGrayscaleRuleCommand(
                        MIGRATION_KEY,
                        created.getRuleId(),
                        null,
                        null,
                        null)));

        assertEquals(ErrorCode.PARAM_ERROR.getCode(), exception.getCode());
    }

    @Test
    void updateEnable_shouldThrowWhenRuleMissing() {
        BizException exception = assertThrows(
                BizException.class,
                () -> service.updateEnable(new UpdateGrayscaleRuleEnableCommand(MIGRATION_KEY, "missing-rule", true)));
        assertEquals(ErrorCode.NOT_FOUND.getCode(), exception.getCode());
    }

    @Test
    void delete_shouldThrowWhenRuleMissing() {
        BizException exception = assertThrows(
                BizException.class,
                () -> service.delete(new DeleteGrayscaleRuleCommand(MIGRATION_KEY, "missing-rule")));
        assertEquals(ErrorCode.NOT_FOUND.getCode(), exception.getCode());
    }

    @Test
    void list_shouldRejectInvalidMigrationKeyFormat() {
        String tooLongMigrationKey = "k".repeat(129);

        BizException tooLongException = assertThrows(
                BizException.class,
                () -> service.list(new ListGrayscaleRuleCommand(tooLongMigrationKey, 1, 10)));
        BizException withWhitespaceException = assertThrows(
                BizException.class,
                () -> service.list(new ListGrayscaleRuleCommand("order sync", 1, 10)));
        BizException withTabException = assertThrows(
                BizException.class,
                () -> service.list(new ListGrayscaleRuleCommand("order	sync", 1, 10)));

        assertEquals(ErrorCode.PARAM_ERROR.getCode(), tooLongException.getCode());
        assertEquals(ErrorCode.PARAM_ERROR.getCode(), withWhitespaceException.getCode());
        assertEquals(ErrorCode.PARAM_ERROR.getCode(), withTabException.getCode());
    }


    @Test
    void commandApis_shouldThrowWhenCommandIsNull() {
        BizException createEx = assertThrows(BizException.class, () -> service.create(null));
        BizException updateEx = assertThrows(BizException.class, () -> service.update(null));
        BizException deleteEx = assertThrows(BizException.class, () -> service.delete(null));
        BizException updateEnableEx = assertThrows(BizException.class, () -> service.updateEnable(null));
        BizException listEx = assertThrows(BizException.class, () -> service.list(null));
        BizException countEx = assertThrows(BizException.class, () -> service.count(null));

        assertEquals(ErrorCode.PARAM_ERROR.getCode(), createEx.getCode());
        assertEquals(ErrorCode.PARAM_ERROR.getCode(), updateEx.getCode());
        assertEquals(ErrorCode.PARAM_ERROR.getCode(), deleteEx.getCode());
        assertEquals(ErrorCode.PARAM_ERROR.getCode(), updateEnableEx.getCode());
        assertEquals(ErrorCode.PARAM_ERROR.getCode(), listEx.getCode());
        assertEquals(ErrorCode.PARAM_ERROR.getCode(), countEx.getCode());
    }

}
