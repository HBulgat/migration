package top.bulgat.migration.admin.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import top.bulgat.common.exception.BizException;
import top.bulgat.common.exception.ErrorCode;
import top.bulgat.migration.admin.application.command.CountDiffRecordCommand;
import top.bulgat.migration.admin.application.command.DetailDiffRecordCommand;
import top.bulgat.migration.admin.application.command.ListDiffRecordCommand;
import top.bulgat.migration.admin.application.command.StatisticsDiffRecordCommand;
import top.bulgat.migration.admin.domain.model.DiffItem;
import top.bulgat.migration.admin.domain.model.DiffRecord;
import top.bulgat.migration.admin.domain.model.DiffType;
import top.bulgat.migration.admin.infrastructure.repository.InMemoryDiffRecordRepository;
import top.bulgat.migration.admin.domain.service.MigrationTaskDomainService;

class DiffRecordQueryApplicationServiceTest {

    private static final String MIGRATION_KEY = "order.sync";

    private InMemoryDiffRecordRepository repository;
    private DiffRecordQueryApplicationService service;

    @BeforeEach
    void setUp() {
        repository = new InMemoryDiffRecordRepository();
        service = new DiffRecordQueryApplicationService(repository, new MigrationTaskDomainService());
    }

    @Test
    void commandApis_shouldDelegateListCountDetailAndStatistics() {
        repository.save(newRecord(MIGRATION_KEY, true, 100, 120, 220, LocalDateTime.of(2026, 2, 20, 10, 0)));

        ListDiffRecordCommand listCommand = new ListDiffRecordCommand(
                MIGRATION_KEY,
                1,
                LocalDate.of(2026, 2, 19),
                LocalDate.of(2026, 2, 22),
                1,
                10);
        CountDiffRecordCommand countCommand = new CountDiffRecordCommand(
                MIGRATION_KEY,
                1,
                LocalDate.of(2026, 2, 19),
                LocalDate.of(2026, 2, 22));

        List<DiffRecord> page = service.list(listCommand);
        long total = service.count(countCommand);
        DiffRecord detail = service.detail(new DetailDiffRecordCommand(page.get(0).getId()));
        DiffRecordQueryApplicationService.DiffStatistics statistics =
                service.statistics(new StatisticsDiffRecordCommand(
                        MIGRATION_KEY,
                        LocalDate.of(2026, 2, 19),
                        LocalDate.of(2026, 2, 22)));

        assertEquals(1, page.size());
        assertEquals(1L, total);
        assertEquals(page.get(0).getId(), detail.getId());
        assertEquals(1L, statistics.totalCount());
    }

    @Test
    void listAndCount_shouldApplyFiltersAndPagination() {
        repository.save(newRecord(MIGRATION_KEY, true, 100, 120, 220, LocalDateTime.of(2026, 2, 20, 10, 0)));
        repository.save(newRecord(MIGRATION_KEY, false, 90, 95, 185, LocalDateTime.of(2026, 2, 21, 10, 0)));
        repository.save(newRecord("other.task", true, 80, 85, 165, LocalDateTime.of(2026, 2, 22, 10, 0)));

        ListDiffRecordCommand command = new ListDiffRecordCommand(
                MIGRATION_KEY,
                1,
                LocalDate.of(2026, 2, 19),
                LocalDate.of(2026, 2, 22),
                1,
                10);
        List<DiffRecord> page = service.list(command);

        assertEquals(1, page.size());
        assertTrue(page.get(0).isHasDiff());
        assertEquals(1L, service.count(new CountDiffRecordCommand(
                MIGRATION_KEY,
                1,
                LocalDate.of(2026, 2, 19),
                LocalDate.of(2026, 2, 22))));
    }

    @Test
    void detail_shouldThrowWhenRecordNotFound() {
        BizException exception = assertThrows(
                BizException.class,
                () -> service.detail(new DetailDiffRecordCommand(999L)));
        assertEquals(ErrorCode.NOT_FOUND.getCode(), exception.getCode());
    }

    @Test
    void statistics_shouldCalculateRateAndAverageCost() {
        repository.save(newRecord(MIGRATION_KEY, true, 100, 200, 300, LocalDateTime.of(2026, 2, 20, 10, 0)));
        repository.save(newRecord(MIGRATION_KEY, false, null, 100, 100, LocalDateTime.of(2026, 2, 20, 11, 0)));
        repository.save(newRecord(MIGRATION_KEY, true, 200, null, 200, LocalDateTime.of(2026, 2, 20, 12, 0)));

        DiffRecordQueryApplicationService.DiffStatistics stats = service.statistics(new StatisticsDiffRecordCommand(
                MIGRATION_KEY,
                LocalDate.of(2026, 2, 20),
                LocalDate.of(2026, 2, 20)));

        assertEquals(3L, stats.totalCount());
        assertEquals(2L, stats.diffCount());
        assertEquals(2.0 / 3.0, stats.diffRate(), 1e-9);
        assertEquals(150, stats.avgOldCostTime());
        assertEquals(150, stats.avgNewCostTime());
    }

    @Test
    void list_shouldThrowWhenStartDateLaterThanEndDate() {
        BizException exception = assertThrows(
                BizException.class,
                () -> service.list(new ListDiffRecordCommand(
                        MIGRATION_KEY,
                        null,
                        LocalDate.of(2026, 2, 22),
                        LocalDate.of(2026, 2, 20),
                        1,
                        10)));

        assertEquals(ErrorCode.PARAM_ERROR.getCode(), exception.getCode());
    }

    @Test
    void list_shouldThrowWhenHasDiffFilterInvalid() {
        BizException exception = assertThrows(
                BizException.class,
                () -> service.list(new ListDiffRecordCommand(
                        MIGRATION_KEY,
                        2,
                        LocalDate.of(2026, 2, 20),
                        LocalDate.of(2026, 2, 22),
                        1,
                        10)));

        assertEquals(ErrorCode.PARAM_ERROR.getCode(), exception.getCode());
    }

    @Test
    void list_shouldThrowWhenPageSizeOutOfRange() {
        BizException exception = assertThrows(
                BizException.class,
                () -> service.list(new ListDiffRecordCommand(
                        MIGRATION_KEY,
                        null,
                        LocalDate.of(2026, 2, 20),
                        LocalDate.of(2026, 2, 22),
                        1,
                        300)));

        assertEquals(ErrorCode.PARAM_ERROR.getCode(), exception.getCode());
    }

    @Test
    void statistics_shouldThrowWhenStartDateLaterThanEndDate() {
        BizException exception = assertThrows(
                BizException.class,
                () -> service.statistics(new StatisticsDiffRecordCommand(
                        MIGRATION_KEY,
                        LocalDate.of(2026, 2, 23),
                        LocalDate.of(2026, 2, 20))));

        assertEquals(ErrorCode.PARAM_ERROR.getCode(), exception.getCode());
    }

    @Test
    void listAndStatistics_shouldRejectInvalidMigrationKeyFormat() {
        String tooLongMigrationKey = "k".repeat(129);

        BizException tooLongException = assertThrows(
                BizException.class,
                () -> service.list(new ListDiffRecordCommand(
                        tooLongMigrationKey,
                        null,
                        LocalDate.of(2026, 2, 20),
                        LocalDate.of(2026, 2, 22),
                        1,
                        10)));
        BizException withWhitespaceException = assertThrows(
                BizException.class,
                () -> service.statistics(new StatisticsDiffRecordCommand(
                        "order sync",
                        LocalDate.of(2026, 2, 20),
                        LocalDate.of(2026, 2, 22))));
        BizException withTabException = assertThrows(
                BizException.class,
                () -> service.statistics(new StatisticsDiffRecordCommand(
                        "order	sync",
                        LocalDate.of(2026, 2, 20),
                        LocalDate.of(2026, 2, 22))));

        assertEquals(ErrorCode.PARAM_ERROR.getCode(), tooLongException.getCode());
        assertEquals(ErrorCode.PARAM_ERROR.getCode(), withWhitespaceException.getCode());
        assertEquals(ErrorCode.PARAM_ERROR.getCode(), withTabException.getCode());
    }

    private DiffRecord newRecord(
            String migrationKey,
            boolean hasDiff,
            Integer oldCost,
            Integer newCost,
            Integer totalCost,
            LocalDateTime createTime) {
        return new DiffRecord(
                0,
                migrationKey,
                "trace-" + createTime.toLocalTime(),
                "{\"old\":1}",
                "{\"new\":2}",
                List.of(new DiffItem("$.amount", "1", "2", DiffType.MODIFY)),
                hasDiff,
                "MODIFY",
                "{}",
                oldCost,
                newCost,
                totalCost,
                createTime);
    }


    @Test
    void commandApis_shouldThrowWhenCommandIsNull() {
        BizException listEx = assertThrows(BizException.class, () -> service.list(null));
        BizException countEx = assertThrows(BizException.class, () -> service.count(null));
        BizException detailEx = assertThrows(BizException.class, () -> service.detail(null));
        BizException statisticsEx = assertThrows(BizException.class, () -> service.statistics(null));

        assertEquals(ErrorCode.PARAM_ERROR.getCode(), listEx.getCode());
        assertEquals(ErrorCode.PARAM_ERROR.getCode(), countEx.getCode());
        assertEquals(ErrorCode.PARAM_ERROR.getCode(), detailEx.getCode());
        assertEquals(ErrorCode.PARAM_ERROR.getCode(), statisticsEx.getCode());
    }

}
