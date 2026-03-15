package top.bulgat.migration.admin.infrastructure.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Repository;
import top.bulgat.migration.admin.domain.model.DiffRule;
import top.bulgat.migration.admin.domain.repository.DiffRuleRepository;
import top.bulgat.migration.config.common.dal.DiffRuleConfigDAO;
import top.bulgat.migration.config.common.model.dataobject.DiffRuleConfig;
import top.bulgat.migration.config.common.model.enums.DiffRuleType;

/**
 * 基于配置中心的Diff规则仓储实现。
 */
@Repository
public class DefaultDiffRuleRepository implements DiffRuleRepository {

    private final DiffRuleConfigDAO diffRuleConfigDAO;

    public DefaultDiffRuleRepository(DiffRuleConfigDAO diffRuleConfigDAO) {
        this.diffRuleConfigDAO = diffRuleConfigDAO;
    }

    @Override
    public DiffRule save(DiffRule rule) {
        List<DiffRule> rules = findByMigrationKey(rule.getMigrationKey());
        rules.removeIf(r -> r.getRuleId().equals(rule.getRuleId()));
        rules.add(rule);

        List<DiffRuleConfig> configs = rules.stream().map(this::toConfig).collect(Collectors.toList());
        diffRuleConfigDAO.save(rule.getMigrationKey(), configs);
        return rule;
    }

    @Override
    public List<DiffRule> findByMigrationKey(String migrationKey) {
        List<DiffRuleConfig> configs = diffRuleConfigDAO.findByMigrationKey(migrationKey);
        return configs.stream().map(this::toEntity).collect(Collectors.toList());
    }

    @Override
    public void deleteByRuleId(String migrationKey, String ruleId) {
        List<DiffRule> rules = findByMigrationKey(migrationKey);
        boolean removed = rules.removeIf(r -> r.getRuleId().equals(ruleId));
        if (removed) {
            List<DiffRuleConfig> configs = rules.stream().map(this::toConfig).collect(Collectors.toList());
            diffRuleConfigDAO.save(migrationKey, configs);
        }
    }

    @Override
    public void deleteByMigrationKey(String migrationKey) {
        diffRuleConfigDAO.delete(migrationKey);
    }

    private DiffRule toEntity(DiffRuleConfig config) {
        return new DiffRule(
                config.migrationKey(),
                config.ruleId(),
                DiffRuleType.fromValue(config.ruleType()),
                config.fieldPath(),
                config.ruleValue(),
                config.enable(),
                config.weight() != null ? config.weight() : 0,
                config.createTime() != null ? config.createTime() : LocalDateTime.now(),
                config.updateTime() != null ? config.updateTime() : LocalDateTime.now());
    }

    private DiffRuleConfig toConfig(DiffRule rule) {
        return new DiffRuleConfig(
                rule.getMigrationKey(),
                rule.getRuleId(),
                rule.getRuleType().name(),
                rule.getFieldPath(),
                rule.getRuleValue(),
                rule.isEnable(),
                rule.getWeight() != null ? rule.getWeight() : 0,
                rule.getCreateTime() != null ? rule.getCreateTime() : LocalDateTime.now(),
                rule.getUpdateTime() != null ? rule.getUpdateTime() : LocalDateTime.now());
    }
}
