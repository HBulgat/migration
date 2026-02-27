package top.bulgat.migration.diff.infrastructure.configcenter;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Repository;
import top.bulgat.migration.diff.domain.model.DiffRule;
import top.bulgat.migration.diff.domain.repository.DiffRuleRepository;

/**
 * InMemoryDiffRuleRepository defines persistence access.
 */
@ConditionalOnMissingBean(DiffRuleRepository.class)
@Repository
public class InMemoryDiffRuleRepository implements DiffRuleRepository {

    private final Map<String, List<DiffRule>> rulesByMigrationKey = new ConcurrentHashMap<>();

    /**
     * Execute findEnabledRules business logic.
     * @param migrationKey migration key.
     * @return result value.
     */
    @Override
    public List<DiffRule> findEnabledRules(String migrationKey) {
        return rulesByMigrationKey.getOrDefault(migrationKey, List.of()).stream()
                .filter(DiffRule::isEnable)
                .collect(Collectors.toList());
    }
}
