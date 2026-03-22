package top.bulgat.migration.admin.domain.repository;

import java.util.List;
import java.util.Optional;
import top.bulgat.migration.admin.domain.model.GrayRule;

/**
 * GrayRuleRepository 定义持久化访问能力。
 */
public interface GrayRuleRepository {

    GrayRule save(GrayRule rule);

    Optional<GrayRule> findByMigrationKeyAndRuleId(String migrationKey, String ruleId);

    List<GrayRule> findByMigrationKey(String migrationKey);

    void deleteByMigrationKeyAndRuleId(String migrationKey, String ruleId);

    void deleteByMigrationKey(String migrationKey);
}
