package top.bulgat.migration.diff.interfaces.assembler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import top.bulgat.migration.diff.application.command.ExecuteDiffCommand;
import top.bulgat.migration.diff.domain.model.DiffItem;
import top.bulgat.migration.diff.domain.model.DiffResult;
import top.bulgat.migration.diff.domain.model.DiffType;
import top.bulgat.migration.diff.interfaces.dto.DiffExecuteRequest;
import top.bulgat.migration.diff.interfaces.dto.DiffExecuteResponse;

class DiffCommandAssemblerTest {

    private final DiffCommandAssembler assembler = new DiffCommandAssembler();

    @Test
    void toCommand_shouldMapRequestFields() {
        DiffExecuteRequest request = new DiffExecuteRequest(
                "user.query",
                "trace-1",
                "{\"a\":1}",
                "{\"a\":2}",
                10,
                11,
                "{\"uid\":1}",
                true,
                false,
                "old-error",
                "new-error",
                "{\"old\":1}",
                "{\"new\":2}",
                3,
                "{\"rule\":\"gray\"}",
                true,
                false);

        ExecuteDiffCommand command = assembler.toCommand(request);

        assertEquals("user.query", command.migrationKey());
        assertEquals("trace-1", command.traceId());
        assertEquals("{\"a\":1}", command.oldJson());
        assertEquals("{\"a\":2}", command.newJson());
        assertEquals(true, command.oldSuccess());
        assertEquals(false, command.newSuccess());
        assertEquals(3, command.MigrationTaskStatus());
        assertEquals("{\"rule\":\"gray\"}", command.grayscaleRules());
        assertEquals(false, command.fallbackTriggered());
    }

    @Test
    void toResponse_shouldMapDomainResultFields() {
        DiffResult result = new DiffResult(
                true,
                List.of(new DiffItem("$.name", "tom", "tommy", DiffType.MODIFY)),
                8L);

        DiffExecuteResponse response = assembler.toResponse(result);

        assertTrue(response.hasDiff());
        assertEquals(1, response.diffResults().size());
        assertEquals("$.name", response.diffResults().get(0).path());
        assertEquals("MODIFY", response.diffResults().get(0).diffType());
        assertEquals(8L, response.costTimeMs());
    }
}
