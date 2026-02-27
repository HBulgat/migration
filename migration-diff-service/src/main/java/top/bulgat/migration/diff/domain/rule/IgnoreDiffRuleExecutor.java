package top.bulgat.migration.diff.domain.rule;

import org.springframework.stereotype.Component;
import top.bulgat.migration.diff.domain.model.DiffItem;
import top.bulgat.migration.diff.domain.model.DiffRule;
import top.bulgat.migration.diff.domain.model.DiffRuleType;

/**
 * IGNORE规则执行器。
 * 命中规则的差异项直接忽略，不再上报。
 */
@Component
public class IgnoreDiffRuleExecutor implements DiffRuleExecutor {

    /**
     * 返回当前执行器支持的规则类型。
     *
     * @return IGNORE
     */
    @Override
    public DiffRuleType supports() {
        return DiffRuleType.IGNORE;
    }

    /**
     * 命中IGNORE规则时始终不返回差异。
     *
     * @param item 差异项
     * @param rule 忽略规则
     * @return false
     */
    @Override
    public boolean shouldReport(DiffItem item, DiffRule rule) {
        return false;
    }
}

