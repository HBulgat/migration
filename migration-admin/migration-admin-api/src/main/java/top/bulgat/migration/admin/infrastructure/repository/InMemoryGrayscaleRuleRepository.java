package top.bulgat.migration.admin.infrastructure.repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;
import top.bulgat.migration.admin.domain.model.GrayscaleRule;
import top.bulgat.migration.admin.domain.repository.GrayscaleRuleRepository;

/**
 * InMemoryGrayscaleRuleRepository defines persistence access.
 */
@Repository
public class InMemoryGrayscaleRuleRepository implements GrayscaleRuleRepository {

    private final Map<String, Map<String, GrayscaleRule>> ruleStore = new ConcurrentHashMap<>();

    /**
     * Persist data.
     * @param rule rule entity.
     * @return 返回结果。
     */
    @Override
    public GrayscaleRule save(GrayscaleRule rule) {
        ruleStore.computeIfAbsent(rule.getMigrationKey(), key -> new ConcurrentHashMap<>())
                .put(rule.getRuleId(), rule);
        return rule;
    }

    /**
     * 执行 findByMigrationKeyAndRuleId 业务逻辑。
     * @param migrationKey migration key.
     * @param ruleId record id.
     * @return 返回结果。
     */
    @Override
    public Optional<GrayscaleRule> findByMigrationKeyAndRuleId(String migrationKey, String ruleId) {
        Map<String, GrayscaleRule> rules = ruleStore.get(migrationKey);
        if (rules == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(rules.get(ruleId));
    }

    /**
     * 执行 findByMigrationKey 业务逻辑。
     * @param migrationKey migration key.
     * @return 返回结果。
     */
    @Override
    public List<GrayscaleRule> findByMigrationKey(String migrationKey) {
        Map<String, GrayscaleRule> rules = ruleStore.get(migrationKey);
        if (rules == null) {
            return List.of();
        }
        return new ArrayList<>(rules.values());
    }

    /**
     * 执行 deleteByMigrationKeyAndRuleId 业务逻辑。
     * @param migrationKey migration key.
     * @param ruleId record id.
     */
    @Override
    public void deleteByMigrationKeyAndRuleId(String migrationKey, String ruleId) {
        Map<String, GrayscaleRule> rules = ruleStore.get(migrationKey);
        if (rules == null) {
            return;
        }
        rules.remove(ruleId);
    }

    /**
     * 执行 deleteByMigrationKey 业务逻辑。
     * @param migrationKey migration key.
     */
    @Override
    public void deleteByMigrationKey(String migrationKey) {
        ruleStore.remove(migrationKey);
    }
}
