package top.bulgat.migration.diff.domain.rule;

import org.springframework.stereotype.Component;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import top.bulgat.migration.diff.domain.model.DiffItem;
import top.bulgat.migration.diff.domain.model.DiffRule;
import top.bulgat.migration.diff.domain.model.DiffRuleType;

/**
 * SCRIPT规则执行器。
 * 基于SpEL表达式判断差异项是否上报。
 */
@Component
public class ScriptDiffRuleExecutor implements DiffRuleExecutor {

    private static final ExpressionParser PARSER = new SpelExpressionParser();

    /**
     * 返回当前执行器支持的规则类型。
     *
     * @return SCRIPT
     */
    @Override
    public DiffRuleType supports() {
        return DiffRuleType.SCRIPT;
    }

    /**
     * 执行SpEL脚本并决定是否上报当前差异项。
     *
     * @param item 差异项
     * @param rule 脚本规则
     * @return true 表示上报；false 表示忽略
     */
    @Override
    public boolean shouldReport(DiffItem item, DiffRule rule) {
        StandardEvaluationContext context = new StandardEvaluationContext();
        context.setVariable("oldValue", item.oldValue());
        context.setVariable("newValue", item.newValue());
        context.setVariable("fieldPath", item.fieldPath());
        context.setVariable("diffType", item.diffType().name());
        try {
            Expression expression = PARSER.parseExpression(rule.ruleValue());
            Boolean shouldReport = expression.getValue(context, Boolean.class);
            return Boolean.TRUE.equals(shouldReport);
        } catch (Exception ex) {
            // 脚本解析异常时，保守上报差异，避免误忽略。
            return true;
        }
    }
}
