package top.bulgat.migration.admin.domain.repository;

import top.bulgat.migration.admin.domain.model.AlertRule;
import java.util.List;

/**
 * AlertRuleRepository 定义持久化访问能力。
 */
public interface AlertRuleRepository {
    AlertRule save(AlertRule rule);
    List<AlertRule> findByMigrationKey(String migrationKey);
    void deleteByRuleId(String migrationKey, String ruleId);
    void deleteByMigrationKey(String migrationKey);
}
