package top.bulgat.migration.diff.domain.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import top.bulgat.common.base.exception.BizException;
import top.bulgat.common.base.exception.ErrorCode;
import top.bulgat.migration.diff.domain.model.DiffRequest;
import top.bulgat.migration.diff.domain.model.DiffResult;
import top.bulgat.migration.diff.domain.model.DiffRule;
import top.bulgat.migration.diff.domain.model.DiffRuleType;
import top.bulgat.migration.diff.domain.rule.DiffRuleExecutorRegistry;
import top.bulgat.migration.diff.domain.rule.IgnoreDiffRuleExecutor;
import top.bulgat.migration.diff.domain.rule.ScriptDiffRuleExecutor;
import top.bulgat.migration.diff.domain.rule.ToleranceDiffRuleExecutor;

class DiffDomainServiceTest {

    private final DiffDomainService service = new DiffDomainService(
            new ObjectMapper(),
            new DiffRuleExecutorRegistry(List.of(
                    new IgnoreDiffRuleExecutor(),
                    new ToleranceDiffRuleExecutor(),
                    new ScriptDiffRuleExecutor())));

    @Test
    void execute_shouldIgnoreArrayOrderWhenSortRuleEnabled() {
        DiffRequest request = new DiffRequest(
                "order-sync",
                "trace-1",
                "{\"items\":[{\"id\":2,\"name\":\"b\"},{\"id\":1,\"name\":\"a\"}]}",
                "{\"items\":[{\"id\":1,\"name\":\"a\"},{\"id\":2,\"name\":\"b\"}]}",
                12,
                10,
                "{}");
        DiffRule sortRule = new DiffRule("order-sync", DiffRuleType.SORT, "$.items", "id", true);

        DiffResult result = service.execute(request, List.of(sortRule));

        assertFalse(result.hasDiff());
        assertEquals(0, result.getDiffItems().size());
    }

    @Test
    void execute_shouldFilterDiffBySpelScriptRule() {
        DiffRequest request = new DiffRequest(
                "price-sync",
                "trace-2",
                "{\"amount\":100}",
                "{\"amount\":105}",
                11,
                9,
                "{}");
        String script = "T(java.lang.Math).abs("
                + "T(java.lang.Double).parseDouble(#newValue) - "
                + "T(java.lang.Double).parseDouble(#oldValue)"
                + ") > 10";
        DiffRule scriptRule = new DiffRule("price-sync", DiffRuleType.SCRIPT, "$.amount", script, true);

        DiffResult result = service.execute(request, List.of(scriptRule));

        assertFalse(result.hasDiff());
        assertEquals(0, result.getDiffItems().size());
    }

    @Test
    void execute_shouldReportDiffWhenScriptRuleIsInvalid() {
        DiffRequest request = new DiffRequest(
                "price-sync",
                "trace-3",
                "{\"amount\":100}",
                "{\"amount\":130}",
                8,
                7,
                "{}");
        DiffRule invalidRule = new DiffRule("price-sync", DiffRuleType.SCRIPT, "$.amount", "#{", true);

        DiffResult result = service.execute(request, List.of(invalidRule));

        assertTrue(result.hasDiff());
        assertEquals(1, result.getDiffItems().size());
    }

    @Test
    void execute_shouldNotMatchSiblingFieldWhenRuleUsesObjectWildcard() {
        DiffRequest request = new DiffRequest(
                "profile-sync",
                "trace-4",
                "{\"a\":{\"name\":\"old\"},\"ab\":{\"name\":\"old\"}}",
                "{\"a\":{\"name\":\"new\"},\"ab\":{\"name\":\"new\"}}",
                6,
                5,
                "{}");
        DiffRule ignoreSubPathRule = new DiffRule("profile-sync", DiffRuleType.IGNORE, "$.a.*", "", true);

        DiffResult result = service.execute(request, List.of(ignoreSubPathRule));

        assertTrue(result.hasDiff());
        assertEquals(1, result.getDiffItems().size());
        assertEquals("ab.name", result.getDiffItems().get(0).getFieldPath());
    }

    @Test
    void execute_shouldThrowBizExceptionWhenJsonPayloadInvalid() {
        DiffRequest request = new DiffRequest(
                "order-sync",
                "trace-5",
                "{",
                "{\"a\":1}",
                10,
                8,
                "{}");

        BizException exception = assertThrows(BizException.class, () -> service.execute(request, List.of()));

        assertEquals(ErrorCode.PARAM_ERROR.getCode(), exception.getCode());
        assertEquals("invalid json payload", exception.getMessage());
    }

    @Test
    void execute_shouldIgnoreArrayElementChildrenWhenRuleUsesArrayWildcardObjectWildcard() {
        DiffRequest request = new DiffRequest(
                "order-sync",
                "trace-6",
                "{\"items\":[{\"id\":1,\"name\":\"a\"},{\"id\":2,\"name\":\"b\"}]}",
                "{\"items\":[{\"id\":1,\"name\":\"x\"},{\"id\":2,\"name\":\"y\"}]}",
                9,
                8,
                "{}");
        DiffRule ignoreRule = new DiffRule("order-sync", DiffRuleType.IGNORE, "$.items[*].*", "", true);

        DiffResult result = service.execute(request, List.of(ignoreRule));

        assertFalse(result.hasDiff(), "unexpected diff paths: "
                + result.getDiffItems().stream().map(item -> item.getFieldPath()).toList());
        assertEquals(0, result.getDiffItems().size());
    }

    @Test
    void execute_shouldIgnoreOnlyMatchedFieldWhenRuleUsesArrayWildcardPath() {
        DiffRequest request = new DiffRequest(
                "order-sync",
                "trace-7",
                "{\"items\":[{\"name\":\"a\",\"nickname\":\"old\"}]}",
                "{\"items\":[{\"name\":\"b\",\"nickname\":\"new\"}]}",
                10,
                9,
                "{}");
        DiffRule ignoreRule = new DiffRule("order-sync", DiffRuleType.IGNORE, "$.items[*].name", "", true);

        DiffResult result = service.execute(request, List.of(ignoreRule));

        assertTrue(result.hasDiff());
        assertEquals(1, result.getDiffItems().size());
        assertEquals("items[0].nickname", result.getDiffItems().get(0).getFieldPath());
    }

    @Test
    void execute_shouldIgnoreArrayElementNodeWhenRuleUsesArrayWildcardPathOnly() {
        DiffRequest request = new DiffRequest(
                "order-sync",
                "trace-8",
                "{\"items\":[{\"id\":1}]}",
                "{\"items\":[{\"id\":2}]}",
                10,
                9,
                "{}");
        DiffRule ignoreRule = new DiffRule("order-sync", DiffRuleType.IGNORE, "$.items[*].id", "", true);

        DiffResult result = service.execute(request, List.of(ignoreRule));

        assertFalse(result.hasDiff());
        assertEquals(0, result.getDiffItems().size());
    }


    @Test
    void execute_shouldSupportRulePathWithoutDollarPrefix() {
        DiffRequest request = new DiffRequest(
                "order-sync",
                "trace-9",
                "{\"items\":[{\"name\":\"a\"}]}",
                "{\"items\":[{\"name\":\"b\"}]}",
                7,
                6,
                "{}");
        DiffRule ignoreRule = new DiffRule("order-sync", DiffRuleType.IGNORE, "items[*].name", "", true);

        DiffResult result = service.execute(request, List.of(ignoreRule));

        assertFalse(result.hasDiff());
        assertEquals(0, result.getDiffItems().size());
    }


    @Test
    void execute_shouldSortNestedArrayByWildcardPathBeforeDiff() {
        DiffRequest request = new DiffRequest(
                "order-sync",
                "trace-10",
                "{\"groups\":[{\"items\":[{\"id\":2,\"name\":\"b\"},{\"id\":1,\"name\":\"a\"}]}]}",
                "{\"groups\":[{\"items\":[{\"id\":1,\"name\":\"a\"},{\"id\":2,\"name\":\"b\"}]}]}",
                12,
                11,
                "{}");
        DiffRule sortRule = new DiffRule("order-sync", DiffRuleType.SORT, "$.groups[*].items", "id", true);

        DiffResult result = service.execute(request, List.of(sortRule));

        assertFalse(result.hasDiff());
        assertEquals(0, result.getDiffItems().size());
    }

    @Test
    void execute_shouldSortArrayByJsonPathFieldBeforeDiff() {
        DiffRequest request = new DiffRequest(
                "order-sync",
                "trace-11",
                "{\"items\":[{\"user\":{\"id\":2},\"name\":\"b\"},{\"user\":{\"id\":1},\"name\":\"a\"}]}",
                "{\"items\":[{\"user\":{\"id\":1},\"name\":\"a\"},{\"user\":{\"id\":2},\"name\":\"b\"}]}",
                10,
                10,
                "{}");
        DiffRule sortRule = new DiffRule("order-sync", DiffRuleType.SORT, "$.items", "$.user.id", true);

        DiffResult result = service.execute(request, List.of(sortRule));

        assertFalse(result.hasDiff());
        assertEquals(0, result.getDiffItems().size());
    }

}


