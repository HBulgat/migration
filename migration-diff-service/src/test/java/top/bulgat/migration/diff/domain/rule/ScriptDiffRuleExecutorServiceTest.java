package top.bulgat.migration.diff.domain.rule;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import top.bulgat.migration.diff.domain.model.DiffItem;
import top.bulgat.migration.diff.domain.model.DiffRule;
import top.bulgat.migration.diff.domain.model.DiffRuleType;
import top.bulgat.migration.diff.domain.model.DiffType;

class ScriptDiffRuleExecutorServiceTest {

    private final ScriptDiffRuleExecutor scriptDiffRuleExecutor = new ScriptDiffRuleExecutor();

    @Test
    void testShouldReport_WhenExpressionEvaluatesFalse_ShouldReturnFalse() {
        DiffItem item = new DiffItem("note", "OLD", "NEW", DiffType.MODIFY);
        DiffRule rule = new DiffRule("demo_key", DiffRuleType.SCRIPT, "note", "false", true, 0);

        boolean shouldReport = scriptDiffRuleExecutor.shouldReport(item, rule);

        Assertions.assertFalse(shouldReport);
    }

    @Test
    void testShouldReport_WhenExpressionHasSyntaxError_ShouldReturnTrue() {
        DiffItem item = new DiffItem("note", "OLD", "NEW", DiffType.MODIFY);
        DiffRule rule = new DiffRule("demo_key", DiffRuleType.SCRIPT, "note", "#oldValue !=", true, 0);

        boolean shouldReport = scriptDiffRuleExecutor.shouldReport(item, rule);

        Assertions.assertTrue(shouldReport);
    }
}
