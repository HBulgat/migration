package top.bulgat.migration.diff.domain.rule;

import org.springframework.stereotype.Component;
import top.bulgat.migration.diff.domain.model.DiffItem;
import top.bulgat.migration.diff.domain.model.DiffRule;
import top.bulgat.migration.diff.domain.model.DiffRuleType;
import top.bulgat.migration.diff.domain.model.DiffType;

/**
 * TOLERANCE规则执行器。
 * 当差异值在容忍区间内时忽略该差异项。
 */
@Component
public class ToleranceDiffRuleExecutor implements DiffRuleExecutor {

    /**
     * 返回当前执行器支持的规则类型。
     *
     * @return TOLERANCE
     */
    @Override
    public DiffRuleType supports() {
        return DiffRuleType.TOLERANCE;
    }

    /**
     * 按容忍阈值判断差异项是否需要上报。
     *
     * @param item 差异项
     * @param rule 容忍规则
     * @return true 表示上报；false 表示忽略
     */
    @Override
    public boolean shouldReport(DiffItem item, DiffRule rule) {
        if (item.getDiffType() != DiffType.MODIFY) {
            return true;
        }
        try {
            double oldValue = Double.parseDouble(item.getOldValue());
            double newValue = Double.parseDouble(item.getNewValue());
            double tolerance = Double.parseDouble(rule.getRuleValue());
            return Math.abs(oldValue - newValue) > tolerance;
        } catch (Exception ex) {
            return true;
        }
    }
}

