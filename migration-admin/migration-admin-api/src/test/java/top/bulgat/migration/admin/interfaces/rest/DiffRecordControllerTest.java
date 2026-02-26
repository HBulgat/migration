package top.bulgat.migration.admin.interfaces.rest;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import top.bulgat.common.exception.BizException;
import top.bulgat.common.exception.ErrorCode;
import top.bulgat.common.springboot.middleware.exception.GlobalExceptionHandler;
import top.bulgat.migration.admin.application.command.CountDiffRecordCommand;
import top.bulgat.migration.admin.application.command.DetailDiffRecordCommand;
import top.bulgat.migration.admin.application.command.ListDiffRecordCommand;
import top.bulgat.migration.admin.application.command.StatisticsDiffRecordCommand;
import top.bulgat.migration.admin.application.service.DiffRecordQueryApplicationService;
import top.bulgat.migration.admin.domain.model.DiffRecord;
import top.bulgat.migration.admin.interfaces.assembler.DiffRecordAssembler;
import top.bulgat.migration.admin.interfaces.dto.DiffRecordResponse;
import top.bulgat.migration.admin.interfaces.dto.DiffStatisticsResponse;

@ExtendWith(MockitoExtension.class)
class DiffRecordControllerTest {

    private MockMvc mockMvc;

    @Mock
    private DiffRecordQueryApplicationService queryApplicationService;

    @Mock
    private DiffRecordAssembler assembler;

    @InjectMocks
    private DiffRecordController controller;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void list_shouldReturnPageData() throws Exception {
        LocalDateTime now = LocalDateTime.of(2026, 2, 23, 21, 0);
        DiffRecordResponse response = new DiffRecordResponse(
                1L,
                "user.query",
                "trace-1",
                "{\"old\":1}",
                "{\"new\":2}",
                List.of(),
                true,
                "MODIFY",
                "{}",
                100,
                120,
                220,
                now);

        ListDiffRecordCommand listCommand = new ListDiffRecordCommand(
                "user.query", 1, LocalDate.of(2026, 2, 20), LocalDate.of(2026, 2, 23), 1, 10);
        CountDiffRecordCommand countCommand = new CountDiffRecordCommand(
                "user.query", 1, LocalDate.of(2026, 2, 20), LocalDate.of(2026, 2, 23));
        when(assembler.toListCommand("user.query", 1, LocalDate.of(2026, 2, 20), LocalDate.of(2026, 2, 23), 1, 10))
                .thenReturn(listCommand);
        when(assembler.toCountCommand("user.query", 1, LocalDate.of(2026, 2, 20), LocalDate.of(2026, 2, 23)))
                .thenReturn(countCommand);
        when(queryApplicationService.count(countCommand)).thenReturn(1L);
        when(queryApplicationService.list(listCommand)).thenReturn(List.of());
        when(assembler.toResponseList(List.of())).thenReturn(List.of(response));

        mockMvc.perform(get("/api/v1/diff_record/list")
                        .param("migration_key", "user.query")
                        .param("has_diff", "1")
                        .param("start_date", "2026-02-20")
                        .param("end_date", "2026-02-23")
                        .param("page", "1")
                        .param("page_size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].id").value(1));
    }

    @Test
    void detail_shouldReturnRecord() throws Exception {
        LocalDateTime now = LocalDateTime.of(2026, 2, 23, 21, 0);
        DiffRecord record = new DiffRecord(
                2L,
                "user.query",
                "trace-2",
                "{\"old\":1}",
                "{\"new\":3}",
                List.of(),
                true,
                "MODIFY",
                "{}",
                100,
                130,
                230,
                now);
        DiffRecordResponse response = new DiffRecordResponse(
                2L,
                "user.query",
                "trace-2",
                "{\"old\":1}",
                "{\"new\":3}",
                List.of(),
                true,
                "MODIFY",
                "{}",
                100,
                130,
                230,
                now);

        DetailDiffRecordCommand command = new DetailDiffRecordCommand(2L);
        when(assembler.toDetailCommand(2L)).thenReturn(command);
        when(queryApplicationService.detail(command)).thenReturn(record);
        when(assembler.toResponse(record)).thenReturn(response);

        mockMvc.perform(get("/api/v1/diff_record/detail").param("id", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(2))
                .andExpect(jsonPath("$.data.migration_key").value("user.query"));
    }

    @Test
    void statistics_shouldReturnSummary() throws Exception {
        DiffRecordQueryApplicationService.DiffStatistics stats =
                new DiffRecordQueryApplicationService.DiffStatistics(10, 3, 0.3, 90, 100);
        DiffStatisticsResponse response = new DiffStatisticsResponse(10, 3, 0.3, 90, 100);

        StatisticsDiffRecordCommand command = new StatisticsDiffRecordCommand(
                "user.query", LocalDate.of(2026, 2, 20), LocalDate.of(2026, 2, 23));
        when(assembler.toStatisticsCommand("user.query", LocalDate.of(2026, 2, 20), LocalDate.of(2026, 2, 23)))
                .thenReturn(command);
        when(queryApplicationService.statistics(command)).thenReturn(stats);
        when(assembler.toStatisticsResponse(stats)).thenReturn(response);

        mockMvc.perform(get("/api/v1/diff_record/statistics")
                        .param("migration_key", "user.query")
                        .param("start_date", "2026-02-20")
                        .param("end_date", "2026-02-23"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total_count").value(10))
                .andExpect(jsonPath("$.data.diff_count").value(3));
    }

    @Test
    void list_shouldReturnBizErrorWhenDateRangeInvalid() throws Exception {
        ListDiffRecordCommand command = new ListDiffRecordCommand(
                "user.query", null, LocalDate.of(2026, 2, 23), LocalDate.of(2026, 2, 20), 1, 10);
        when(assembler.toListCommand("user.query", null, LocalDate.of(2026, 2, 23), LocalDate.of(2026, 2, 20), 1, 10))
                .thenReturn(command);
        when(queryApplicationService.list(command)).thenThrow(
                new BizException(ErrorCode.PARAM_ERROR, "start_date must not be later than end_date"));

        mockMvc.perform(get("/api/v1/diff_record/list")
                        .param("migration_key", "user.query")
                        .param("start_date", "2026-02-23")
                        .param("end_date", "2026-02-20")
                        .param("page", "1")
                        .param("page_size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ErrorCode.PARAM_ERROR.getCode()))
                .andExpect(jsonPath("$.message").value("start_date must not be later than end_date"));
    }

    @Test
    void list_shouldReturnBizErrorWhenHasDiffOutOfRange() throws Exception {
        ListDiffRecordCommand command = new ListDiffRecordCommand("user.query", 2, null, null, 1, 10);
        when(assembler.toListCommand("user.query", 2, null, null, 1, 10)).thenReturn(command);
        when(queryApplicationService.list(command))
                .thenThrow(new BizException(ErrorCode.PARAM_ERROR, "has_diff must be 0 or 1"));

        mockMvc.perform(get("/api/v1/diff_record/list")
                        .param("migration_key", "user.query")
                        .param("has_diff", "2")
                        .param("page", "1")
                        .param("page_size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ErrorCode.PARAM_ERROR.getCode()))
                .andExpect(jsonPath("$.message").value("has_diff must be 0 or 1"));
    }

    @Test
    void list_shouldReturnBizErrorWhenPaginationInvalid() throws Exception {
        ListDiffRecordCommand command = new ListDiffRecordCommand("user.query", null, null, null, 1, 300);
        when(assembler.toListCommand("user.query", null, null, null, 1, 300)).thenReturn(command);
        when(queryApplicationService.list(command))
                .thenThrow(new BizException(ErrorCode.PARAM_ERROR, "pageSize out of range [1,200]"));

        mockMvc.perform(get("/api/v1/diff_record/list")
                        .param("migration_key", "user.query")
                        .param("page", "1")
                        .param("page_size", "300"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ErrorCode.PARAM_ERROR.getCode()))
                .andExpect(jsonPath("$.message").value("pageSize out of range [1,200]"));
    }

    @Test
    void detail_shouldReturnNotFoundWhenRecordMissing() throws Exception {
        DetailDiffRecordCommand command = new DetailDiffRecordCommand(99L);
        when(assembler.toDetailCommand(99L)).thenReturn(command);
        when(queryApplicationService.detail(command))
                .thenThrow(new BizException(ErrorCode.NOT_FOUND, "diff record not found: 99"));

        mockMvc.perform(get("/api/v1/diff_record/detail").param("id", "99"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ErrorCode.NOT_FOUND.getCode()))
                .andExpect(jsonPath("$.message").value("diff record not found: 99"));
    }

    @Test
    void statistics_shouldReturnBizErrorWhenDateRangeInvalid() throws Exception {
        StatisticsDiffRecordCommand command = new StatisticsDiffRecordCommand(
                "user.query", LocalDate.of(2026, 2, 23), LocalDate.of(2026, 2, 20));
        when(assembler.toStatisticsCommand("user.query", LocalDate.of(2026, 2, 23), LocalDate.of(2026, 2, 20)))
                .thenReturn(command);
        when(queryApplicationService.statistics(command)).thenThrow(
                new BizException(ErrorCode.PARAM_ERROR, "start_date must not be later than end_date"));

        mockMvc.perform(get("/api/v1/diff_record/statistics")
                        .param("migration_key", "user.query")
                        .param("start_date", "2026-02-23")
                        .param("end_date", "2026-02-20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ErrorCode.PARAM_ERROR.getCode()))
                .andExpect(jsonPath("$.message").value("start_date must not be later than end_date"));
    }

}
