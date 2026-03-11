package top.bulgat.migration.diff.infrastructure.repository.config;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import top.bulgat.common.notice.NoticeChannel;
import top.bulgat.migration.config.common.dal.AlertRuleConfigDAO;
import top.bulgat.migration.config.common.model.dataobject.AlertRuleConfig;
import top.bulgat.migration.diff.domain.model.AlertRule;
import top.bulgat.migration.diff.domain.repository.AlertRuleRepository;

import java.util.List;
import java.util.stream.Collectors;

/**
 * DefaultAlertRuleRepository 定义持久化访问能力。
 */
@Repository
@Slf4j
public class DefaultAlertRuleRepository implements AlertRuleRepository {

    private final AlertRuleConfigDAO alertRuleConfigDAO;

    public DefaultAlertRuleRepository(AlertRuleConfigDAO alertRuleConfigDAO) {
        this.alertRuleConfigDAO = alertRuleConfigDAO;
    }

    @Override
    public List<AlertRule> findEnabledRules(String migrationKey) {
        try {
            List<AlertRuleConfig> configs = alertRuleConfigDAO.findByMigrationKey(migrationKey);
            return configs.stream()
                    .filter(c -> c != null && c.enable())
                    .map(this::toEntity)
                    .collect(Collectors.toList());
        } catch (Exception ex) {
            log.warn(
                    "failed to load diff rules from nacos, fallback to empty, migrationKey={}, reason={}",
                    migrationKey,
                    sanitizeReason(ex));
            log.debug("failed to load diff rules detail, migrationKey={}", migrationKey, ex);
            return List.of();
        }
    }

    private AlertRule toEntity(AlertRuleConfig config) {
        return new AlertRule(
                config.migrationKey(),
                config.ruleId(),
                config.name(),
                config.enable(),
                NoticeChannel.fromValue(config.channel()),
                config.templateKey(),
                config.receivers());
    }

    private String sanitizeReason(Exception ex) {
        if (ex == null || ex.getMessage() == null) {
            return "unknown";
        }
        return ex.getMessage().replaceAll("\\s+", " ").trim();
    }
}
