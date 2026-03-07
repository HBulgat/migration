package top.bulgat.migration.diff.domain.repository;

import java.util.List;
import top.bulgat.migration.diff.domain.model.DiffRule;

/**
 * DiffRuleRepository 定义持久化访问能力。
 */
public interface DiffRuleRepository {

    List<DiffRule> findEnabledRules(String migrationKey);
}

