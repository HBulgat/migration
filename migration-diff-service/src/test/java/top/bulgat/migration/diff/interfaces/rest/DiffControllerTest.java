package top.bulgat.migration.diff.interfaces.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import top.bulgat.common.base.exception.BizException;
import top.bulgat.common.base.exception.ErrorCode;
import top.bulgat.common.springboot.middleware.exception.GlobalExceptionHandler;
import top.bulgat.migration.diff.application.command.ExecuteDiffCommand;
import top.bulgat.migration.diff.application.service.DiffApplicationService;
import top.bulgat.migration.diff.domain.model.DiffResult;
import top.bulgat.migration.diff.interfaces.assembler.DiffCommandAssembler;
import top.bulgat.migration.diff.interfaces.dto.DiffExecuteResponse;

@ExtendWith(MockitoExtension.class)
class DiffControllerTest {

    private MockMvc mockMvc;

    @Mock
    private DiffApplicationService applicationService;

    @Mock
    private DiffCommandAssembler assembler;

    @InjectMocks
    private DiffController controller;

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
    void execute_shouldReturnDiffResult() throws Exception {
        ExecuteDiffCommand command = new ExecuteDiffCommand(
                "user.query",
                "trace-1",
                "{\"a\":1}",
                "{\"a\":2}",
                10,
                11,
                "{}");
        DiffResult domainResult = new DiffResult(true, List.of(), 5L);
        DiffExecuteResponse response = new DiffExecuteResponse(true, List.of(), 5L);

        when(assembler.toCommand(any())).thenReturn(command);
        when(applicationService.executeDiff(command)).thenReturn(domainResult);
        when(assembler.toResponse(domainResult)).thenReturn(response);

        mockMvc.perform(post("/api/v1/diff")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "migration_key": "user.query",
                                  "trace_id": "trace-1",
                                  "old_json": "{\\"a\\":1}",
                                  "new_json": "{\\"a\\":2}",
                                  "old_cost_time_ms": 10,
                                  "new_cost_time_ms": 11,
                                  "grayscale_param": "{}"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.has_diff").value(true))
                .andExpect(jsonPath("$.data.cost_time_ms").value(5));
    }

    @Test
    void execute_shouldReturnBadRequestWhenOldJsonMissing() throws Exception {
        mockMvc.perform(post("/api/v1/diff")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "migration_key": "user.query",
                                  "new_json": "{\\"a\\":2}"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void execute_shouldReturnBizErrorResultWhenBusinessExceptionRaised() throws Exception {
        ExecuteDiffCommand command = new ExecuteDiffCommand(
                "user.query",
                "trace-2",
                "{\"a\":1}",
                "{\"a\":2}",
                8,
                7,
                "{}");
        when(assembler.toCommand(any())).thenReturn(command);
        when(applicationService.executeDiff(command))
                .thenThrow(new BizException(ErrorCode.PARAM_ERROR, "invalid diff payload"));

        mockMvc.perform(post("/api/v1/diff")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "migration_key": "user.query",
                                  "trace_id": "trace-2",
                                  "old_json": "{\\"a\\":1}",
                                  "new_json": "{\\"a\\":2}"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ErrorCode.PARAM_ERROR.getCode()))
                .andExpect(jsonPath("$.message").value("invalid diff payload"));
    }

    @Test
    void execute_shouldReturnBadRequestWhenCostTimeNegative() throws Exception {
        mockMvc.perform(post("/api/v1/diff")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "migration_key": "user.query",
                                  "trace_id": "trace-3",
                                  "old_json": "{\\"a\\":1}",
                                  "new_json": "{\\"a\\":2}",
                                  "old_cost_time_ms": -1
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }


    @Test
    void execute_shouldReturnBadRequestWhenMigrationKeyTooLong() throws Exception {
        String longMigrationKey = "x".repeat(129);

        mockMvc.perform(post("/api/v1/diff")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "migration_key": "%s",
                                  "old_json": "{\\"a\\":1}",
                                  "new_json": "{\\"a\\":2}"
                                }
                                """.formatted(longMigrationKey)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void execute_shouldReturnBadRequestWhenMigrationKeyContainsWhitespace() throws Exception {
        mockMvc.perform(post("/api/v1/diff")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "migration_key": "user query",
                                  "old_json": "{\\"a\\":1}",
                                  "new_json": "{\\"a\\":2}"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

}
