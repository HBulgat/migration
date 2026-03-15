package top.bulgat.migration.diff.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;
import top.bulgat.common.base.exception.BizException;
import top.bulgat.common.base.exception.ErrorCode;
import top.bulgat.migration.diff.application.command.ExecuteDiffCommand;
import top.bulgat.migration.diff.domain.model.DiffRequest;
import top.bulgat.migration.diff.domain.model.DiffResult;
import top.bulgat.migration.diff.domain.model.DiffRule;
import top.bulgat.migration.diff.domain.model.DiffRuleType;
import top.bulgat.migration.diff.domain.repository.DiffRecordRepository;
import top.bulgat.migration.diff.domain.repository.DiffRuleRepository;
import top.bulgat.migration.diff.domain.service.AlertService;
import top.bulgat.migration.diff.domain.service.DiffDomainService;

class Test {

    private DiffDomainService diffDomainService;
    private DiffRecordRepository diffRecordRepository;
    private DiffRuleRepository diffRuleRepository;
    private AlertService alertService;
    private DiffApplicationService service;

    @BeforeEach
    void setUp() {
        diffDomainService = Mockito.mock(DiffDomainService.class);
        diffRecordRepository = Mockito.mock(DiffRecordRepository.class);
        diffRuleRepository = Mockito.mock(DiffRuleRepository.class);
        alertService = Mockito.mock(AlertService.class);
        service = new DiffApplicationService(diffDomainService, diffRecordRepository, diffRuleRepository, alertService);
    }

    @org.junit.jupiter.api.Test
    void executeDiff_shouldThrowWhenMigrationKeyMissing() {
        ExecuteDiffCommand command = createCommand(
                " ",
                "trace-1",
                "{\"a\":1}",
                "{\"a\":2}",
                10,
                8,
                "{}");

        BizException exception = assertThrows(BizException.class, () -> service.executeDiff(command));

        assertEquals(ErrorCode.PARAM_ERROR.getCode(), exception.getCode());
    }

    @org.junit.jupiter.api.Test
    void executeDiff_shouldThrowWhenCommandNull() {
        BizException exception = assertThrows(BizException.class, () -> service.executeDiff(null));
        assertEquals(ErrorCode.PARAM_ERROR.getCode(), exception.getCode());
    }

    @org.junit.jupiter.api.Test
    void executeDiff_shouldAllowBlankOldJson() {
        ExecuteDiffCommand command = createCommand(
                "user.query",
                "trace-1",
                "",
                "{\"a\":2}",
                10,
                8,
                "{}");
        DiffResult result = new DiffResult(false, List.of(), 3L);
        when(diffRuleRepository.findEnabledRules("user.query")).thenReturn(List.of());
        when(diffDomainService.execute(any(DiffRequest.class), eq(List.of()))).thenReturn(result);

        DiffResult actual = assertDoesNotThrow(() -> service.executeDiff(command));

        assertEquals(result, actual);
    }

    @org.junit.jupiter.api.Test
    void executeDiff_shouldAllowBlankNewJson() {
        ExecuteDiffCommand command = createCommand(
                "user.query",
                "trace-1",
                "{\"a\":1}",
                " ",
                10,
                8,
                "{}");
        DiffResult result = new DiffResult(false, List.of(), 4L);
        when(diffRuleRepository.findEnabledRules("user.query")).thenReturn(List.of());
        when(diffDomainService.execute(any(DiffRequest.class), eq(List.of()))).thenReturn(result);

        DiffResult actual = assertDoesNotThrow(() -> service.executeDiff(command));

        assertEquals(result, actual);
    }

    @org.junit.jupiter.api.Test
    void executeDiff_shouldThrowWhenMigrationKeyTooLong() {
        String longMigrationKey = "x".repeat(129);
        ExecuteDiffCommand command = createCommand(
                longMigrationKey,
                "trace-1",
                "{\"a\":1}",
                "{\"a\":2}",
                10,
                8,
                "{}");

        BizException exception = assertThrows(BizException.class, () -> service.executeDiff(command));

        assertEquals(ErrorCode.PARAM_ERROR.getCode(), exception.getCode());
    }

    @org.junit.jupiter.api.Test
    void executeDiff_shouldThrowWhenMigrationKeyContainsSpace() {
        ExecuteDiffCommand command = createCommand(
                "user query",
                "trace-1",
                "{\"a\":1}",
                "{\"a\":2}",
                10,
                8,
                "{}");

        BizException exception = assertThrows(BizException.class, () -> service.executeDiff(command));

        assertEquals(ErrorCode.PARAM_ERROR.getCode(), exception.getCode());
    }

    @org.junit.jupiter.api.Test
    void executeDiff_shouldThrowWhenMigrationKeyContainsTab() {
        ExecuteDiffCommand command = createCommand(
                "user	query",
                "trace-1",
                "{\"a\":1}",
                "{\"a\":2}",
                10,
                8,
                "{}");

        BizException exception = assertThrows(BizException.class, () -> service.executeDiff(command));

        assertEquals(ErrorCode.PARAM_ERROR.getCode(), exception.getCode());
    }

    @org.junit.jupiter.api.Test
    void executeDiff_shouldThrowWhenCostTimeNegative() {
        ExecuteDiffCommand command = createCommand(
                "user.query",
                "trace-1",
                "{\"a\":1}",
                "{\"a\":2}",
                -1,
                8,
                "{}");

        BizException exception = assertThrows(BizException.class, () -> service.executeDiff(command));

        assertEquals(ErrorCode.PARAM_ERROR.getCode(), exception.getCode());
    }

    @org.junit.jupiter.api.Test
    void executeDiff_shouldThrowWhenNewCostTimeNegative() {
        ExecuteDiffCommand command = createCommand(
                "user.query",
                "trace-1",
                "{\"a\":1}",
                "{\"a\":2}",
                10,
                -1,
                "{}");

        BizException exception = assertThrows(BizException.class, () -> service.executeDiff(command));

        assertEquals(ErrorCode.PARAM_ERROR.getCode(), exception.getCode());
    }

    @org.junit.jupiter.api.Test
    void executeDiff_shouldLoadRulesAndPersistRecord() {
        ExecuteDiffCommand command = createCommand(
                "user.query",
                "trace-2",
                "{\"a\":1}",
                "{\"a\":2}",
                10,
                12,
                "{}");
        List<DiffRule> rules = List.of(new DiffRule(
                "user.query",
                DiffRuleType.IGNORE,
                "$.a",
                "",
                true,
                0));

        DiffResult result = new DiffResult(true, List.of(), 5L);
        when(diffRuleRepository.findEnabledRules("user.query")).thenReturn(rules);
        when(diffDomainService.execute(any(DiffRequest.class), eq(rules))).thenReturn(result);

        DiffResult actual = service.executeDiff(command);

        assertEquals(result, actual);
        verify(diffRuleRepository).findEnabledRules("user.query");
        verify(diffDomainService).execute(any(DiffRequest.class), eq(rules));
        verify(diffRecordRepository).save(any(DiffRequest.class), eq(result));
    }

    private ExecuteDiffCommand createCommand(
            String migrationKey,
            String traceId,
            String oldJson,
            String newJson,
            Integer oldCostTimeMs,
            Integer newCostTimeMs,
            String grayscaleParam) {
        return new ExecuteDiffCommand(
                migrationKey,
                traceId,
                oldJson,
                newJson,
                oldCostTimeMs,
                newCostTimeMs,
                grayscaleParam,
                true,
                true,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null);
    }
}
