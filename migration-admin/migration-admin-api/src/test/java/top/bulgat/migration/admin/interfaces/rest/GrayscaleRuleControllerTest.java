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
import top.bulgat.common.base.exception.BizException;
import top.bulgat.common.base.exception.ErrorCode;
import top.bulgat.common.springboot.middleware.exception.GlobalExceptionHandler;
import top.bulgat.migration.admin.application.command.CreateGrayscaleRuleCommand;
import top.bulgat.migration.admin.application.command.DeleteGrayscaleRuleCommand;
import top.bulgat.migration.admin.application.command.ListGrayscaleRuleCommand;
import top.bulgat.migration.admin.application.command.UpdateGrayscaleRuleCommand;
import top.bulgat.migration.admin.application.command.UpdateGrayscaleRuleEnableCommand;
import top.bulgat.migration.admin.application.service.GrayscaleRuleApplicationService;
import top.bulgat.migration.admin.domain.model.GrayscaleRule;
import top.bulgat.migration.admin.domain.model.GrayscaleRuleType;
import top.bulgat.migration.admin.interfaces.assembler.GrayscaleRuleAssembler;
import top.bulgat.migration.admin.interfaces.dto.GrayscaleRuleResponse;

@ExtendWith(MockitoExtension.class)
class GrayscaleRuleControllerTest {

    private MockMvc mockMvc;

    @Mock
    private GrayscaleRuleApplicationService applicationService;

    @Mock
    private GrayscaleRuleAssembler assembler;

    @InjectMocks
    private GrayscaleRuleController controller;

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
    void create_shouldReturnRuleResponse() throws Exception {
        LocalDateTime now = LocalDateTime.of(2026, 2, 23, 21, 0);
        GrayscaleRule rule = new GrayscaleRule(
                "rule-1",
                "user.query",
                GrayscaleRuleType.WHITELIST,
                "[\"u1\"]",
                true,
                now,
                now);
        GrayscaleRuleResponse response = new GrayscaleRuleResponse(
                "rule-1",
                "user.query",
                "WHITELIST",
                "[\"u1\"]",
                true,
                now,
                now);

        CreateGrayscaleRuleCommand command = new CreateGrayscaleRuleCommand("user.query", "WHITELIST", "[\"u1\"]", true);
        when(assembler.toCreateCommand(any())).thenReturn(command);
        when(applicationService.create(command)).thenReturn(rule);
        when(assembler.toResponse(rule)).thenReturn(response);

        mockMvc.perform(post("/api/v1/grayscale_rule/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "migration_key": "user.query",
                                  "rule_type": "WHITELIST",
                                  "rule_value": "[\\"u1\\"]",
                                  "enable": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.rule_id").value("rule-1"));
    }

    @Test
    void updateEnable_shouldInvokeApplicationService() throws Exception {
        UpdateGrayscaleRuleEnableCommand command = new UpdateGrayscaleRuleEnableCommand("user.query", "rule-1", false);
        when(assembler.toUpdateEnableCommand(any())).thenReturn(command);
        mockMvc.perform(post("/api/v1/grayscale_rule/update_enable")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "migration_key": "user.query",
                                  "rule_id": "rule-1",
                                  "enable": false
                                }
                                """))
                .andExpect(status().isOk());

        verify(applicationService).updateEnable(command);
    }

    @Test
    void list_shouldReturnPageData() throws Exception {
        LocalDateTime now = LocalDateTime.of(2026, 2, 23, 21, 0);
        GrayscaleRule rule = new GrayscaleRule(
                "rule-1",
                "user.query",
                GrayscaleRuleType.WHITELIST,
                "[\"u1\"]",
                true,
                now,
                now);
        GrayscaleRuleResponse response = new GrayscaleRuleResponse(
                "rule-1",
                "user.query",
                "WHITELIST",
                "[\"u1\"]",
                true,
                now,
                now);

        ListGrayscaleRuleCommand command = new ListGrayscaleRuleCommand("user.query", 1, 10);
        when(assembler.toListCommand("user.query", 1, 10)).thenReturn(command);
        when(applicationService.count(command)).thenReturn(1L);
        when(applicationService.list(command)).thenReturn(List.of(rule));
        when(assembler.toResponseList(List.of(rule))).thenReturn(List.of(response));

        mockMvc.perform(get("/api/v1/grayscale_rule/list")
                        .param("migration_key", "user.query")
                        .param("page", "1")
                        .param("page_size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].rule_id").value("rule-1"));
    }

    @Test
    void updateEnable_shouldReturnBadRequestWhenEnableMissing() throws Exception {
        mockMvc.perform(post("/api/v1/grayscale_rule/update_enable")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "migration_key": "user.query",
                                  "rule_id": "rule-1"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void create_shouldReturnBizErrorWhenRuleTypeInvalid() throws Exception {
        CreateGrayscaleRuleCommand command = new CreateGrayscaleRuleCommand("user.query", "INVALID", "[\"u1\"]", true);
        when(assembler.toCreateCommand(any())).thenReturn(command);
        when(applicationService.create(command))
                .thenThrow(new BizException(ErrorCode.PARAM_ERROR, "invalid rule type"));

        mockMvc.perform(post("/api/v1/grayscale_rule/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "migration_key": "user.query",
                                  "rule_type": "INVALID",
                                  "rule_value": "[\\"u1\\"]",
                                  "enable": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ErrorCode.PARAM_ERROR.getCode()))
                .andExpect(jsonPath("$.message").value("invalid rule type"));
    }

    @Test
    void list_shouldReturnBizErrorWhenPaginationInvalid() throws Exception {
        ListGrayscaleRuleCommand command = new ListGrayscaleRuleCommand("user.query", 1, 300);
        when(assembler.toListCommand("user.query", 1, 300)).thenReturn(command);
        when(applicationService.list(command))
                .thenThrow(new BizException(ErrorCode.PARAM_ERROR, "pageSize out of range [1,200]"));

        mockMvc.perform(get("/api/v1/grayscale_rule/list")
                        .param("migration_key", "user.query")
                        .param("page", "1")
                        .param("page_size", "300"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ErrorCode.PARAM_ERROR.getCode()))
                .andExpect(jsonPath("$.message").value("pageSize out of range [1,200]"));
    }

    @Test
    void update_shouldReturnNotFoundWhenRuleMissing() throws Exception {
        UpdateGrayscaleRuleCommand command = new UpdateGrayscaleRuleCommand(
                "user.query", "missing-rule", "WHITELIST", "[\"u1\"]", true);
        when(assembler.toUpdateCommand(any())).thenReturn(command);
        doThrow(new BizException(ErrorCode.NOT_FOUND, "grayscale rule not found: missing-rule"))
                .when(applicationService)
                .update(command);

        mockMvc.perform(post("/api/v1/grayscale_rule/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "migration_key": "user.query",
                                  "rule_id": "missing-rule",
                                  "rule_type": "WHITELIST",
                                  "rule_value": "[\\"u1\\"]",
                                  "enable": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ErrorCode.NOT_FOUND.getCode()))
                .andExpect(jsonPath("$.message").value("grayscale rule not found: missing-rule"));
    }

    @Test
    void update_shouldReturnBizErrorWhenNoFieldProvided() throws Exception {
        UpdateGrayscaleRuleCommand command = new UpdateGrayscaleRuleCommand("user.query", "rule-1", null, null, null);
        when(assembler.toUpdateCommand(any())).thenReturn(command);
        doThrow(new BizException(
                ErrorCode.PARAM_ERROR,
                "at least one field(rule_type/rule_value/enable) must be provided"))
                .when(applicationService)
                .update(command);

        mockMvc.perform(post("/api/v1/grayscale_rule/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "migration_key": "user.query",
                                  "rule_id": "rule-1"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ErrorCode.PARAM_ERROR.getCode()))
                .andExpect(jsonPath("$.message").value(
                        "at least one field(rule_type/rule_value/enable) must be provided"));
    }

    @Test
    void updateEnable_shouldReturnNotFoundWhenRuleMissing() throws Exception {
        UpdateGrayscaleRuleEnableCommand command = new UpdateGrayscaleRuleEnableCommand("user.query", "missing-rule", true);
        when(assembler.toUpdateEnableCommand(any())).thenReturn(command);
        doThrow(new BizException(ErrorCode.NOT_FOUND, "grayscale rule not found: missing-rule"))
                .when(applicationService)
                .updateEnable(command);

        mockMvc.perform(post("/api/v1/grayscale_rule/update_enable")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "migration_key": "user.query",
                                  "rule_id": "missing-rule",
                                  "enable": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ErrorCode.NOT_FOUND.getCode()))
                .andExpect(jsonPath("$.message").value("grayscale rule not found: missing-rule"));
    }

    @Test
    void delete_shouldReturnNotFoundWhenRuleMissing() throws Exception {
        DeleteGrayscaleRuleCommand command = new DeleteGrayscaleRuleCommand("user.query", "missing-rule");
        when(assembler.toDeleteCommand(any())).thenReturn(command);
        doThrow(new BizException(ErrorCode.NOT_FOUND, "grayscale rule not found: missing-rule"))
                .when(applicationService)
                .delete(command);

        mockMvc.perform(post("/api/v1/grayscale_rule/delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "migration_key": "user.query",
                                  "rule_id": "missing-rule"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ErrorCode.NOT_FOUND.getCode()))
                .andExpect(jsonPath("$.message").value("grayscale rule not found: missing-rule"));
    }


    @Test
    void create_shouldReturnBadRequestWhenMigrationKeyTooLong() throws Exception {
        String longMigrationKey = "x".repeat(129);

        mockMvc.perform(post("/api/v1/grayscale_rule/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "migration_key": "%s",
                                  "rule_type": "WHITELIST",
                                  "rule_value": "[\\"u1\\"]",
                                  "enable": true
                                }
                                """.formatted(longMigrationKey)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

}
