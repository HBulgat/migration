package top.bulgat.migration.admin.infrastructure.repository;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;
import top.bulgat.migration.admin.domain.model.GrayscaleRule;
import top.bulgat.migration.admin.domain.model.GrayscaleRuleType;
import top.bulgat.migration.admin.domain.repository.GrayscaleRuleRepository;
import top.bulgat.migration.admin.infrastructure.configcenter.ConfigCenterGateway;

/**
 * ConfigCenterGrayscaleRuleRepository defines persistence access.
 */
@Primary
@Repository
public class ConfigCenterGrayscaleRuleRepository implements GrayscaleRuleRepository {

    private static final String RULE_DATA_ID_PREFIX = "migration_";
    private static final String LEGACY_RULE_DATA_ID_PREFIX = "grayscale_";
    private static final String RULE_GROUP = "GRAYSCALE_RULE_GROUP";

    private final ConfigCenterGateway configCenterGateway;
    private final ObjectMapper objectMapper;

    public ConfigCenterGrayscaleRuleRepository(ConfigCenterGateway configCenterGateway, ObjectMapper objectMapper) {
        this.configCenterGateway = configCenterGateway;
        this.objectMapper = objectMapper;
    }

    /**
     * Persist data.
     * @param rule rule entity.
     * @return 返回结果。
     */
    @Override
    public GrayscaleRule save(GrayscaleRule rule) {
        List<GrayscaleRule> rules = findByMigrationKey(rule.getMigrationKey());
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
        persistRules(rule.getMigrationKey(), rules);
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
        return findByMigrationKey(migrationKey).stream()
                .filter(rule -> rule.getRuleId().equals(ruleId))
                .findFirst();
    }

    /**
     * 执行 findByMigrationKey 业务逻辑。
     * @param migrationKey migration key.
     * @return 返回结果。
     */
    @Override
    public List<GrayscaleRule> findByMigrationKey(String migrationKey) {
        try {
            Optional<String> content = configCenterGateway.getConfig(RULE_DATA_ID_PREFIX + migrationKey, RULE_GROUP);
            if (content.isEmpty() || content.get().isBlank()) {
                content = configCenterGateway.getConfig(
                        RULE_DATA_ID_PREFIX + migrationKey,
                        ConfigCenterGateway.DEFAULT_GROUP);
            }
            if (content.isEmpty() || content.get().isBlank()) {
                content = configCenterGateway.getConfig(
                        LEGACY_RULE_DATA_ID_PREFIX + migrationKey,
                        ConfigCenterGateway.DEFAULT_GROUP);
            }
            if (content.isEmpty() || content.get().isBlank()) {
                return new ArrayList<>();
            }
            List<RuleConfig> configs = objectMapper.readValue(content.get(), new TypeReference<List<RuleConfig>>() {
            });
            List<GrayscaleRule> rules = new ArrayList<>();
            for (RuleConfig config : configs) {
                rules.add(new GrayscaleRule(
                        config.ruleId(),
                        config.migrationKey(),
                        GrayscaleRuleType.fromValue(config.ruleType()),
                        config.ruleValue(),
                        config.enable(),
                        config.createTime() == null ? LocalDateTime.now() : config.createTime(),
                        config.updateTime() == null ? LocalDateTime.now() : config.updateTime()));
            }
            return rules;
        } catch (Exception ex) {
            throw new IllegalStateException("failed to load grayscale rules", ex);
        }
    }

    /**
     * 执行 deleteByMigrationKeyAndRuleId 业务逻辑。
     * @param migrationKey migration key.
     * @param ruleId record id.
     */
    @Override
    public void deleteByMigrationKeyAndRuleId(String migrationKey, String ruleId) {
        List<GrayscaleRule> rules = findByMigrationKey(migrationKey);
        rules.removeIf(rule -> rule.getRuleId().equals(ruleId));
        persistRules(migrationKey, rules);
    }

    /**
     * 执行 deleteByMigrationKey 业务逻辑。
     * @param migrationKey migration key.
     */
    @Override
    public void deleteByMigrationKey(String migrationKey) {
        configCenterGateway.delete(RULE_DATA_ID_PREFIX + migrationKey, RULE_GROUP);
        configCenterGateway.delete(RULE_DATA_ID_PREFIX + migrationKey, ConfigCenterGateway.DEFAULT_GROUP);
        configCenterGateway.delete(LEGACY_RULE_DATA_ID_PREFIX + migrationKey, ConfigCenterGateway.DEFAULT_GROUP);
    }

    private void persistRules(String migrationKey, List<GrayscaleRule> rules) {
        try {
            List<RuleConfig> payload = new ArrayList<>();
            for (GrayscaleRule rule : rules) {
                payload.add(new RuleConfig(
                        rule.getRuleId(),
                        rule.getMigrationKey(),
                        rule.getRuleType().name(),
                        rule.getRuleValue(),
                        rule.isEnable(),
                        rule.getCreateTime(),
                        rule.getUpdateTime()));
            }
            configCenterGateway.publish(
                    RULE_DATA_ID_PREFIX + migrationKey,
                    RULE_GROUP,
                    objectMapper.writeValueAsString(payload));
        } catch (Exception ex) {
            throw new IllegalStateException("failed to persist grayscale rules", ex);
        }
    }

    private record RuleConfig(
            @JsonProperty("rule_id") String ruleId,
            @JsonProperty("migration_key") String migrationKey,
            @JsonProperty("rule_type") String ruleType,
            @JsonProperty("rule_value") String ruleValue,
            @JsonProperty("enable") boolean enable,
            @JsonProperty("create_time") LocalDateTime createTime,
            @JsonProperty("update_time") LocalDateTime updateTime) {
    }
}
