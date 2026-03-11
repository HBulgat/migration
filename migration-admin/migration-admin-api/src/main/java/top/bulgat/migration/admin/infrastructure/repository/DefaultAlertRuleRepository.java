package top.bulgat.migration.admin.infrastructure.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Repository;
import top.bulgat.common.notice.NoticeChannel;
import top.bulgat.migration.admin.domain.model.AlertRule;
import top.bulgat.migration.admin.domain.repository.AlertRuleRepository;
import top.bulgat.migration.config.common.dal.AlertRuleConfigDAO;
import top.bulgat.migration.config.common.model.dataobject.AlertRuleConfig;

/**
 * 基于配置中心的告警规则仓储实现。
 */
@Repository
public class DefaultAlertRuleRepository implements AlertRuleRepository {

    private final AlertRuleConfigDAO alertRuleConfigDAO;

    public DefaultAlertRuleRepository(AlertRuleConfigDAO alertRuleConfigDAO) {
        this.alertRuleConfigDAO = alertRuleConfigDAO;
    }

    @Override
    public AlertRule save(AlertRule rule) {
        List<AlertRule> rules = findByMigrationKey(rule.getMigrationKey());
        rules.removeIf(r -> r.getRuleId().equals(rule.getRuleId()));
        rules.add(rule);

        List<AlertRuleConfig> configs = rules.stream().map(this::toConfig).collect(Collectors.toList());
        alertRuleConfigDAO.save(rule.getMigrationKey(), configs);
        return rule;
    }

    @Override
    public List<AlertRule> findByMigrationKey(String migrationKey) {
        List<AlertRuleConfig> configs = alertRuleConfigDAO.findByMigrationKey(migrationKey);
        return configs.stream().map(this::toEntity).collect(Collectors.toList());
    }

    @Override
    public void deleteByRuleId(String migrationKey, String ruleId) {
        List<AlertRule> rules = findByMigrationKey(migrationKey);
        boolean removed = rules.removeIf(r -> r.getRuleId().equals(ruleId));
        if (removed) {
            List<AlertRuleConfig> configs = rules.stream().map(this::toConfig).collect(Collectors.toList());
            alertRuleConfigDAO.save(migrationKey, configs);
        }
    }

    @Override
    public void deleteByMigrationKey(String migrationKey) {
        alertRuleConfigDAO.delete(migrationKey);
    }

    private AlertRule toEntity(AlertRuleConfig config) {
        return new AlertRule(
                config.migrationKey(),
                config.ruleId(),
                config.name(),
                config.enable(),
                NoticeChannel.fromValue(config.channel()),
                config.templateKey(),
                config.receivers(),
                config.createTime() != null ? config.createTime() : LocalDateTime.now(),
                config.updateTime() != null ? config.updateTime() : LocalDateTime.now());
    }

    private AlertRuleConfig toConfig(AlertRule rule) {
        return new AlertRuleConfig(
                rule.getMigrationKey(),
                rule.getRuleId(),
                rule.getName(),
                rule.isEnable(),
                rule.getChannel().name(),
                rule.getTemplateKey(),
                rule.getReceivers(),
                rule.getCreateTime() != null ? rule.getCreateTime() : LocalDateTime.now(),
                rule.getUpdateTime() != null ? rule.getUpdateTime() : LocalDateTime.now());
    }
}
