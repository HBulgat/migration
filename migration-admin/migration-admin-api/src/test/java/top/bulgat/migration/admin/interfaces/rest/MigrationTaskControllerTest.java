package top.bulgat.migration.admin.interfaces.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import top.bulgat.common.exception.BizException;
import top.bulgat.common.exception.ErrorCode;
import top.bulgat.common.springboot.middleware.exception.GlobalExceptionHandler;
import top.bulgat.migration.admin.application.command.CreateMigrationTaskCommand;
import top.bulgat.migration.admin.application.command.DeleteMigrationTaskCommand;
import top.bulgat.migration.admin.application.command.ListMigrationTaskCommand;
import top.bulgat.migration.admin.application.command.QueryMigrationTaskCommand;
import top.bulgat.migration.admin.application.command.UpdateMigrationTaskCommand;
import top.bulgat.migration.admin.application.command.UpdateMigrationTaskStatusCommand;
import top.bulgat.migration.admin.application.service.MigrationTaskApplicationService;
import top.bulgat.migration.admin.domain.model.MigrationStatus;
import top.bulgat.migration.admin.domain.model.MigrationTask;
import top.bulgat.migration.admin.interfaces.assembler.MigrationTaskCommandAssembler;
import top.bulgat.migration.admin.interfaces.dto.MigrationTaskResponse;

@ExtendWith(MockitoExtension.class)
class MigrationTaskControllerTest {

    private MockMvc mockMvc;

    @Mock
    private MigrationTaskApplicationService applicationService;

    @Mock
    private MigrationTaskCommandAssembler assembler;

    @InjectMocks
    private MigrationTaskController controller;

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
    void create_shouldReturnTaskResponse() throws Exception {
        CreateMigrationTaskCommand command = new CreateMigrationTaskCommand("user.query", 1, "desc");
        LocalDateTime now = LocalDateTime.of(2026, 2, 23, 21, 0);
        MigrationTask task = new MigrationTask("user.query", MigrationStatus.OLD, "desc", now, now);
        MigrationTaskResponse response = new MigrationTaskResponse("user.query", 1, "desc", now, now);

        when(assembler.toCommand(any())).thenReturn(command);
        when(applicationService.createMigrationTask(command)).thenReturn(task);
        when(assembler.toResponse(task)).thenReturn(response);

        mockMvc.perform(post("/api/v1/migration_task/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "migration_key": "user.query",
                                  "status": 1,
                                  "description": "desc"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.migration_key").value("user.query"));

        verify(applicationService).createMigrationTask(command);
    }

    @Test
    void create_shouldReturnBadRequestWhenMigrationKeyMissing() throws Exception {
        mockMvc.perform(post("/api/v1/migration_task/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": 1
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void list_shouldReturnPageData() throws Exception {
        LocalDateTime now = LocalDateTime.of(2026, 2, 23, 21, 0);
        MigrationTask task = new MigrationTask("user.query", MigrationStatus.OLD, "desc", now, now);
        MigrationTaskResponse response = new MigrationTaskResponse("user.query", 1, "desc", now, now);

        ListMigrationTaskCommand command = new ListMigrationTaskCommand(null, null, 1, 10);
        when(assembler.toListCommand(null, null, 1, 10)).thenReturn(command);
        when(applicationService.count(command)).thenReturn(1L);
        when(applicationService.list(command)).thenReturn(List.of(task));
        when(assembler.toResponseList(List.of(task))).thenReturn(List.of(response));

        mockMvc.perform(get("/api/v1/migration_task/list")
                        .param("page", "1")
                        .param("page_size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.current").value(1))
                .andExpect(jsonPath("$.data.size").value(10))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].migration_key").value("user.query"));
    }

    @Test
    void query_shouldReturnBizErrorResultWhenTaskMissing() throws Exception {
        QueryMigrationTaskCommand command = new QueryMigrationTaskCommand("missing.task");
        when(assembler.toQueryCommand(any())).thenReturn(command);
        when(applicationService.getByMigrationKey(command))
                .thenThrow(new BizException(ErrorCode.NOT_FOUND, "task missing"));

        mockMvc.perform(post("/api/v1/migration_task/query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "migration_key": "missing.task"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ErrorCode.NOT_FOUND.getCode()))
                .andExpect(jsonPath("$.message").value("task missing"));
    }

    @Test
    void list_shouldReturnBizErrorWhenStatusOutOfRange() throws Exception {
        ListMigrationTaskCommand command = new ListMigrationTaskCommand(9, null, 1, 10);
        when(assembler.toListCommand(9, null, 1, 10)).thenReturn(command);
        when(applicationService.count(command))
                .thenThrow(new BizException(ErrorCode.PARAM_ERROR, "status out of range [1,7]"));

        mockMvc.perform(get("/api/v1/migration_task/list")
                        .param("page", "1")
                        .param("page_size", "10")
                        .param("status", "9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ErrorCode.PARAM_ERROR.getCode()))
                .andExpect(jsonPath("$.message").value("status out of range [1,7]"));
    }

    @Test
    void list_shouldReturnBizErrorWhenPaginationInvalid() throws Exception {
        ListMigrationTaskCommand command = new ListMigrationTaskCommand(null, null, 0, 10);
        when(assembler.toListCommand(null, null, 0, 10)).thenReturn(command);
        when(applicationService.list(command))
                .thenThrow(new BizException(ErrorCode.PARAM_ERROR, "page must be greater than or equal to 1"));

        mockMvc.perform(get("/api/v1/migration_task/list")
                        .param("page", "0")
                        .param("page_size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ErrorCode.PARAM_ERROR.getCode()))
                .andExpect(jsonPath("$.message").value("page must be greater than or equal to 1"));
    }

    @Test
    void updateStatus_shouldReturnNotFoundWhenTaskMissing() throws Exception {
        UpdateMigrationTaskStatusCommand command = new UpdateMigrationTaskStatusCommand("missing.task", 2);
        when(assembler.toUpdateStatusCommand(any())).thenReturn(command);
        when(applicationService.updateStatus(command))
                .thenThrow(new BizException(ErrorCode.NOT_FOUND, "migration task not found: missing.task"));

        mockMvc.perform(post("/api/v1/migration_task/update_status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "migration_key": "missing.task",
                                  "target_status": 2
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ErrorCode.NOT_FOUND.getCode()))
                .andExpect(jsonPath("$.message").value("migration task not found: missing.task"));
    }

    @Test
    void update_shouldReturnBizErrorWhenNoFieldProvided() throws Exception {
        UpdateMigrationTaskCommand command = new UpdateMigrationTaskCommand("user.query", null, null);
        when(assembler.toUpdateCommand(any())).thenReturn(command);
        when(applicationService.updateTask(command))
                .thenThrow(new BizException(
                        ErrorCode.PARAM_ERROR,
                        "at least one field(status/description) must be provided"));

        mockMvc.perform(post("/api/v1/migration_task/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "migration_key": "user.query"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ErrorCode.PARAM_ERROR.getCode()))
                .andExpect(jsonPath("$.message").value("at least one field(status/description) must be provided"));
    }

    @Test
    void delete_shouldReturnNotFoundWhenTaskMissing() throws Exception {
        DeleteMigrationTaskCommand command = new DeleteMigrationTaskCommand("missing.task");
        when(assembler.toDeleteCommand(any())).thenReturn(command);
        doThrow(new BizException(ErrorCode.NOT_FOUND, "migration task not found: missing.task"))
                .when(applicationService)
                .deleteByMigrationKey(command);

        mockMvc.perform(post("/api/v1/migration_task/delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "migration_key": "missing.task"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ErrorCode.NOT_FOUND.getCode()))
                .andExpect(jsonPath("$.message").value("migration task not found: missing.task"));
    }


    @Test
    void create_shouldReturnBadRequestWhenMigrationKeyTooLong() throws Exception {
        String longMigrationKey = "x".repeat(129);

        mockMvc.perform(post("/api/v1/migration_task/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "migration_key": "%s",
                                  "status": 1,
                                  "description": "desc"
                                }
                                """.formatted(longMigrationKey)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void create_shouldReturnBadRequestWhenMigrationKeyContainsWhitespace() throws Exception {
        mockMvc.perform(post("/api/v1/migration_task/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "migration_key": "user query",
                                  "status": 1,
                                  "description": "desc"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

}
