package top.bulgat.migration.admin.infrastructure.repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import top.bulgat.migration.admin.domain.model.DiffRule;
import top.bulgat.migration.admin.domain.repository.DiffRuleRepository;

public class InMemoryDiffRuleRepository implements DiffRuleRepository {

    private final Map<String, DiffRule> store = new ConcurrentHashMap<>();

    @Override
    public DiffRule save(DiffRule rule) {
        store.put(rule.getRuleId(), rule);
        return rule;
    }

    @Override
    public List<DiffRule> findByMigrationKey(String migrationKey) {
        return store.values().stream()
                .filter(r -> r.getMigrationKey().equals(migrationKey))
                .collect(Collectors.toList());
    }

    @Override
    public void deleteByRuleId(String migrationKey, String ruleId) {
        DiffRule rule = store.get(ruleId);
        if (rule != null && rule.getMigrationKey().equals(migrationKey)) {
            store.remove(ruleId);
        }
    }

    @Override
    public void deleteByMigrationKey(String migrationKey) {
        List<String> toRemove = new ArrayList<>();
        store.forEach((k, v) -> {
            if (v.getMigrationKey().equals(migrationKey)) {
                toRemove.add(k);
            }
        });
        toRemove.forEach(store::remove);
    }
}
