package top.bulgat.migration.diff.domain.rule;

import top.bulgat.migration.diff.domain.model.DiffItem;
import top.bulgat.migration.diff.domain.model.DiffRule;
import top.bulgat.migration.diff.domain.model.DiffRuleType;

/**
 * DiffRuleExecutor interface.
 */
public interface DiffRuleExecutor {

    DiffRuleType supports();

    boolean shouldReport(DiffItem item, DiffRule rule);
}

