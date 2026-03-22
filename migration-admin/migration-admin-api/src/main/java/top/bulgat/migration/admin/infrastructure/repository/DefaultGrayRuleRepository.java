package top.bulgat.migration.admin.infrastructure.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Repository;
import top.bulgat.migration.admin.domain.model.GrayRule;
import top.bulgat.migration.admin.domain.repository.GrayRuleRepository;
import top.bulgat.migration.config.common.dal.GrayRuleConfigDAO;
import top.bulgat.migration.config.common.model.dataobject.GrayRuleConfig;
import top.bulgat.migration.config.common.model.enums.GrayRuleType;

/**
 * DefaultGrayRuleRepository 定义持久化访问能力。
 */
@Repository
public class DefaultGrayRuleRepository implements GrayRuleRepository {

    private final GrayRuleConfigDAO grayRuleConfigDAO;

    public DefaultGrayRuleRepository(GrayRuleConfigDAO grayRuleConfigDAO) {
        this.grayRuleConfigDAO = grayRuleConfigDAO;
    }

    @Override
    public GrayRule save(GrayRule rule) {
        List<GrayRule> rules = findByMigrationKey(rule.getMigrationKey());
        boolean replaced = false;
        for (int i = 0; i < rules.size(); i++) {
            if (rules.get(i).getRuleId().equals(rule.getRuleId())) {
                rules.set(i, rule);
                replaced = true;
                break;
            }
        }
        if (!replaced) {
            rules.add(rule);
        }

        List<GrayRuleConfig> configs = rules.stream().map(this::toConfig).collect(Collectors.toList());
        grayRuleConfigDAO.save(rule.getMigrationKey(), configs);
        return rule;
    }

    @Override
    public Optional<GrayRule> findByMigrationKeyAndRuleId(String migrationKey, String ruleId) {
        return findByMigrationKey(migrationKey).stream()
                .filter(rule -> rule.getRuleId().equals(ruleId))
                .findFirst();
    }

    @Override
    public List<GrayRule> findByMigrationKey(String migrationKey) {
        List<GrayRuleConfig> configs = grayRuleConfigDAO.findByMigrationKey(migrationKey);
        return configs.stream().map(this::toEntity).collect(Collectors.toList());
    }

    @Override
    public void deleteByMigrationKeyAndRuleId(String migrationKey, String ruleId) {
        List<GrayRule> rules = findByMigrationKey(migrationKey);
        boolean removed = rules.removeIf(rule -> rule.getRuleId().equals(ruleId));
        if (removed) {
            List<GrayRuleConfig> configs = rules.stream().map(this::toConfig).collect(Collectors.toList());
            grayRuleConfigDAO.save(migrationKey, configs);
        }
    }

    @Override
    public void deleteByMigrationKey(String migrationKey) {
        grayRuleConfigDAO.delete(migrationKey);
    }

    private GrayRule toEntity(GrayRuleConfig config) {
        return new GrayRule(
                config.ruleId(),
                config.migrationKey(),
                GrayRuleType.fromValue(config.ruleType()),
                config.ruleValue(),
                config.enable(),
                config.createTime() == null ? LocalDateTime.now() : config.createTime(),
                config.updateTime() == null ? LocalDateTime.now() : config.updateTime());
    }

    private GrayRuleConfig toConfig(GrayRule rule) {
        return new GrayRuleConfig(
                rule.getRuleId(),
                rule.getMigrationKey(),
                rule.getRuleType().name(),
                rule.getRuleValue(),
                rule.isEnable(),
                rule.getCreateTime() == null ? LocalDateTime.now() : rule.getCreateTime(),
                rule.getUpdateTime() == null ? LocalDateTime.now() : rule.getUpdateTime());
    }
}
