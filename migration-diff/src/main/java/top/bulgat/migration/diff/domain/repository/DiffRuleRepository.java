package top.bulgat.migration.diff.domain.repository;

import java.util.List;
import top.bulgat.migration.diff.domain.model.DiffRule;

/**
 * DiffRuleRepository defines persistence access.
 */
public interface DiffRuleRepository {

    List<DiffRule> findEnabledRules(String migrationKey);
}

