package top.bulgat.migration.diff.infrastructure.repository.config;

import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import top.bulgat.migration.diff.domain.model.DiffRule;
import top.bulgat.migration.diff.domain.model.DiffRuleType;
import top.bulgat.migration.diff.domain.repository.DiffRuleRepository;
import top.bulgat.migration.config.common.dal.DiffRuleConfigDAO;
import top.bulgat.migration.config.common.model.dataobject.DiffRuleConfig;

/**
 * DefaultDiffRuleRepository 定义持久化访问能力。
 */
@Repository
public class DefaultDiffRuleRepository implements DiffRuleRepository {

    private static final Logger log = LoggerFactory.getLogger(DefaultDiffRuleRepository.class);

    private final DiffRuleConfigDAO diffRuleConfigDAO;

    public DefaultDiffRuleRepository(DiffRuleConfigDAO diffRuleConfigDAO) {
        this.diffRuleConfigDAO = diffRuleConfigDAO;
    }

    @Override
    public List<DiffRule> findEnabledRules(String migrationKey) {
        try {
            List<DiffRuleConfig> configs = diffRuleConfigDAO.findByMigrationKey(migrationKey);
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

    private DiffRule toEntity(DiffRuleConfig config) {
        String ruleMigrationKey = config.migrationKey();
        return new DiffRule(
                ruleMigrationKey,
                DiffRuleType.fromValue(config.ruleType()),
                config.fieldPath(),
                config.ruleValue(),
                true);
    }

    private String sanitizeReason(Exception ex) {
        if (ex == null || ex.getMessage() == null) {
            return "unknown";
        }
        return ex.getMessage().replaceAll("\\s+", " ").trim();
    }
}
