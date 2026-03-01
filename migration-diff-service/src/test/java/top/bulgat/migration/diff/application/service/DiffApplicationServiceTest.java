package top.bulgat.migration.diff.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
import top.bulgat.migration.diff.domain.service.DiffDomainService;

class DiffApplicationServiceTest {

    private DiffDomainService diffDomainService;
    private DiffRecordRepository diffRecordRepository;
    private DiffRuleRepository diffRuleRepository;
    private DiffApplicationService service;

    @BeforeEach
    void setUp() {
        diffDomainService = Mockito.mock(DiffDomainService.class);
        diffRecordRepository = Mockito.mock(DiffRecordRepository.class);
        diffRuleRepository = Mockito.mock(DiffRuleRepository.class);
        service = new DiffApplicationService(diffDomainService, diffRecordRepository, diffRuleRepository);
    }

    @Test
    void executeDiff_shouldThrowWhenMigrationKeyMissing() {
        ExecuteDiffCommand command = new ExecuteDiffCommand(
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

    @Test
    void executeDiff_shouldThrowWhenCommandNull() {
        BizException exception = assertThrows(BizException.class, () -> service.executeDiff(null));
        assertEquals(ErrorCode.PARAM_ERROR.getCode(), exception.getCode());
    }

    @Test
    void executeDiff_shouldThrowWhenOldJsonMissing() {
        ExecuteDiffCommand command = new ExecuteDiffCommand(
                "user.query",
                "trace-1",
                "",
                "{\"a\":2}",
                10,
                8,
                "{}");

        BizException exception = assertThrows(BizException.class, () -> service.executeDiff(command));

        assertEquals(ErrorCode.PARAM_ERROR.getCode(), exception.getCode());
    }

    @Test
    void executeDiff_shouldThrowWhenNewJsonMissing() {
        ExecuteDiffCommand command = new ExecuteDiffCommand(
                "user.query",
                "trace-1",
                "{\"a\":1}",
                " ",
                10,
                8,
                "{}");

        BizException exception = assertThrows(BizException.class, () -> service.executeDiff(command));

        assertEquals(ErrorCode.PARAM_ERROR.getCode(), exception.getCode());
    }

    @Test
    void executeDiff_shouldThrowWhenMigrationKeyTooLong() {
        String longMigrationKey = "x".repeat(129);
        ExecuteDiffCommand command = new ExecuteDiffCommand(
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

    @Test
    void executeDiff_shouldThrowWhenMigrationKeyContainsSpace() {
        ExecuteDiffCommand command = new ExecuteDiffCommand(
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

    @Test
    void executeDiff_shouldThrowWhenMigrationKeyContainsTab() {
        ExecuteDiffCommand command = new ExecuteDiffCommand(
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

    @Test
    void executeDiff_shouldThrowWhenCostTimeNegative() {
        ExecuteDiffCommand command = new ExecuteDiffCommand(
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

    @Test
    void executeDiff_shouldThrowWhenNewCostTimeNegative() {
        ExecuteDiffCommand command = new ExecuteDiffCommand(
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

    @Test
    void executeDiff_shouldLoadRulesAndPersistRecord() {
        ExecuteDiffCommand command = new ExecuteDiffCommand(
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
                true));
        DiffResult result = new DiffResult(true, List.of(), 5L);
        when(diffRuleRepository.findEnabledRules("user.query")).thenReturn(rules);
        when(diffDomainService.execute(any(DiffRequest.class), eq(rules))).thenReturn(result);

        DiffResult actual = service.executeDiff(command);

        assertEquals(result, actual);
        verify(diffRuleRepository).findEnabledRules("user.query");
        verify(diffDomainService).execute(any(DiffRequest.class), eq(rules));
        verify(diffRecordRepository).save(any(DiffRequest.class), eq(result));
    }
}
