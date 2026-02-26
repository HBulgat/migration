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
     * @return result value.
     */
    @Override
    public GrayscaleRule save(GrayscaleRule rule) {
        ruleStore.computeIfAbsent(rule.getMigrationKey(), key -> new ConcurrentHashMap<>())
                .put(rule.getRuleId(), rule);
        return rule;
    }

    /**
     * Execute findByMigrationKeyAndRuleId business logic.
     * @param migrationKey migration key.
     * @param ruleId record id.
     * @return result value.
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
     * Execute findByMigrationKey business logic.
     * @param migrationKey migration key.
     * @return result value.
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
     * Execute deleteByMigrationKeyAndRuleId business logic.
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
     * Execute deleteByMigrationKey business logic.
     * @param migrationKey migration key.
     */
    @Override
    public void deleteByMigrationKey(String migrationKey) {
        ruleStore.remove(migrationKey);
    }
}
