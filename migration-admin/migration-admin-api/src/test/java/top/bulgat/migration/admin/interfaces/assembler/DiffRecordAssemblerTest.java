package top.bulgat.migration.admin.interfaces.assembler;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import top.bulgat.migration.admin.application.command.CountDiffRecordCommand;
import top.bulgat.migration.admin.application.command.DetailDiffRecordCommand;
import top.bulgat.migration.admin.application.command.ListDiffRecordCommand;
import top.bulgat.migration.admin.application.command.StatisticsDiffRecordCommand;
import top.bulgat.migration.admin.application.service.DiffRecordQueryApplicationService;
import top.bulgat.migration.admin.domain.model.DiffItem;
import top.bulgat.migration.admin.domain.model.DiffRecord;
import top.bulgat.migration.admin.domain.model.DiffType;
import top.bulgat.migration.admin.interfaces.dto.DiffRecordResponse;
import top.bulgat.migration.admin.interfaces.dto.DiffStatisticsResponse;

class DiffRecordAssemblerTest {

    private final DiffRecordAssembler assembler = new DiffRecordAssembler();

    @Test
    void toCommands_shouldMapQueryArguments() {
        ListDiffRecordCommand listCommand = assembler.toListCommand(
                "user.query",
                1,
                LocalDate.of(2026, 2, 20),
                LocalDate.of(2026, 2, 23),
                1,
                10);
        CountDiffRecordCommand countCommand = assembler.toCountCommand(
                "user.query",
                1,
                LocalDate.of(2026, 2, 20),
                LocalDate.of(2026, 2, 23));
        DetailDiffRecordCommand detailCommand = assembler.toDetailCommand(11L);
        StatisticsDiffRecordCommand statisticsCommand = assembler.toStatisticsCommand(
                "user.query",
                LocalDate.of(2026, 2, 20),
                LocalDate.of(2026, 2, 23));

        assertEquals("user.query", listCommand.migrationKey());
        assertEquals(1, listCommand.page());
        assertEquals(10, listCommand.pageSize());
        assertEquals(1, countCommand.hasDiff());
        assertEquals(11L, detailCommand.id());
        assertEquals(LocalDate.of(2026, 2, 23), statisticsCommand.endDate());
    }


    @Test
    void toResponseAndList_shouldMapRecordFields() {
        LocalDateTime now = LocalDateTime.of(2026, 2, 23, 21, 0);
        DiffRecord record = new DiffRecord(
                11L,
                "user.query",
                "trace-1",
                "{\"old\":1}",
                "{\"new\":2}",
                List.of(new DiffItem("$.name", "tom", "tommy", DiffType.MODIFY)),
                true,
                "MODIFY",
                "{\"uid\":1}",
                100,
                120,
                220,
                now,
                true,
                true,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                false);

        DiffRecordResponse response = assembler.toResponse(record);
        List<DiffRecordResponse> responseList = assembler.toResponseList(List.of(record));

        assertEquals(11L, response.id());
        assertEquals("user.query", response.migrationKey());
        assertEquals("MODIFY", response.diffResults().get(0).diffType());
        assertEquals(1, responseList.size());
    }

    @Test
    void toStatisticsResponse_shouldMapStatisticsFields() {
        DiffRecordQueryApplicationService.DiffStatistics statistics =
                new DiffRecordQueryApplicationService.DiffStatistics(10, 4, 0.4, 91, 99);

        DiffStatisticsResponse response = assembler.toStatisticsResponse(statistics);

        assertEquals(10, response.totalCount());
        assertEquals(4, response.diffCount());
        assertEquals(0.4, response.diffRate());
        assertEquals(91, response.avgOldCostTime());
        assertEquals(99, response.avgNewCostTime());
    }
}
