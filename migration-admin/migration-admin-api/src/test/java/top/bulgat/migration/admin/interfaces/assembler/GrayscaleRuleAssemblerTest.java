package top.bulgat.migration.admin.interfaces.assembler;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import top.bulgat.migration.admin.application.command.CreateGrayscaleRuleCommand;
import top.bulgat.migration.admin.application.command.DeleteGrayscaleRuleCommand;
import top.bulgat.migration.admin.application.command.ListGrayscaleRuleCommand;
import top.bulgat.migration.admin.application.command.UpdateGrayscaleRuleCommand;
import top.bulgat.migration.admin.application.command.UpdateGrayscaleRuleEnableCommand;
import top.bulgat.migration.admin.domain.model.GrayscaleRule;
import top.bulgat.migration.admin.domain.model.GrayscaleRuleType;
import top.bulgat.migration.admin.interfaces.dto.CreateGrayscaleRuleRequest;
import top.bulgat.migration.admin.interfaces.dto.DeleteGrayscaleRuleRequest;
import top.bulgat.migration.admin.interfaces.dto.GrayscaleRuleResponse;
import top.bulgat.migration.admin.interfaces.dto.UpdateGrayscaleRuleEnableRequest;
import top.bulgat.migration.admin.interfaces.dto.UpdateGrayscaleRuleRequest;

class GrayscaleRuleAssemblerTest {

    private final GrayscaleRuleAssembler assembler = new GrayscaleRuleAssembler();

    @Test
    void toCommands_shouldMapRequestFields() {
        CreateGrayscaleRuleRequest createRequest = new CreateGrayscaleRuleRequest("user.query", "WHITELIST", "[\"u1\"]", true);
        UpdateGrayscaleRuleRequest updateRequest = new UpdateGrayscaleRuleRequest("user.query", "rule-1", "WHITELIST", "[\"u2\"]", false);
        DeleteGrayscaleRuleRequest deleteRequest = new DeleteGrayscaleRuleRequest("user.query", "rule-1");
        UpdateGrayscaleRuleEnableRequest enableRequest = new UpdateGrayscaleRuleEnableRequest("user.query", "rule-1", true);

        CreateGrayscaleRuleCommand createCommand = assembler.toCreateCommand(createRequest);
        UpdateGrayscaleRuleCommand updateCommand = assembler.toUpdateCommand(updateRequest);
        DeleteGrayscaleRuleCommand deleteCommand = assembler.toDeleteCommand(deleteRequest);
        UpdateGrayscaleRuleEnableCommand enableCommand = assembler.toUpdateEnableCommand(enableRequest);
        ListGrayscaleRuleCommand listCommand = assembler.toListCommand("user.query", 1, 10);

        assertEquals("user.query", createCommand.migrationKey());
        assertEquals("WHITELIST", createCommand.ruleType());
        assertEquals("rule-1", updateCommand.ruleId());
        assertEquals("[\"u2\"]", updateCommand.ruleValue());
        assertEquals("rule-1", deleteCommand.ruleId());
        assertEquals("rule-1", enableCommand.ruleId());
        assertEquals(10, listCommand.pageSize());
    }


    @Test
    void toResponseAndList_shouldMapRuleFields() {
        LocalDateTime now = LocalDateTime.of(2026, 2, 23, 21, 0);
        GrayscaleRule rule = new GrayscaleRule(
                "rule-1",
                "user.query",
                GrayscaleRuleType.WHITELIST,
                "[\"u1\"]",
                true,
                now,
                now);

        GrayscaleRuleResponse response = assembler.toResponse(rule);
        List<GrayscaleRuleResponse> responseList = assembler.toResponseList(List.of(rule));

        assertEquals("rule-1", response.ruleId());
        assertEquals("user.query", response.migrationKey());
        assertEquals("WHITELIST", response.ruleType());
        assertEquals(1, responseList.size());
        assertEquals("rule-1", responseList.get(0).ruleId());
    }
}

