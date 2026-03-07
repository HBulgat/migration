package top.bulgat.migration.admin.domain.repository;

import java.util.List;
import java.util.Optional;
import top.bulgat.migration.admin.domain.model.GrayscaleRule;

/**
 * GrayscaleRuleRepository 定义持久化访问能力。
 */
public interface GrayscaleRuleRepository {

    GrayscaleRule save(GrayscaleRule rule);

    Optional<GrayscaleRule> findByMigrationKeyAndRuleId(String migrationKey, String ruleId);

    List<GrayscaleRule> findByMigrationKey(String migrationKey);

    void deleteByMigrationKeyAndRuleId(String migrationKey, String ruleId);

    void deleteByMigrationKey(String migrationKey);
}
