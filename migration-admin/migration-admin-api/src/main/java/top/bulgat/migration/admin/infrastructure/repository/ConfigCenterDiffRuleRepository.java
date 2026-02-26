package top.bulgat.migration.admin.infrastructure.repository;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;
import top.bulgat.migration.admin.domain.model.DiffRule;
import top.bulgat.migration.admin.domain.model.DiffRuleType;
import top.bulgat.migration.admin.domain.repository.DiffRuleRepository;
import top.bulgat.migration.admin.infrastructure.configcenter.ConfigCenterGateway;

/**
 * 基于配置中心的Diff规则仓储实现。
 */
@Primary
@Repository
public class ConfigCenterDiffRuleRepository implements DiffRuleRepository {

    private static final Logger log = LoggerFactory.getLogger(ConfigCenterDiffRuleRepository.class);
    private static final String DATA_ID_PREFIX = "diff_rule_";
    private static final String GROUP = "MIGRATION_DIFF_GROUP";

    private final ConfigCenterGateway configCenterGateway;
    private final ObjectMapper objectMapper;

    public ConfigCenterDiffRuleRepository(ConfigCenterGateway configCenterGateway, ObjectMapper objectMapper) {
        this.configCenterGateway = configCenterGateway;
        this.objectMapper = objectMapper;
    }

    @Override
    public DiffRule save(DiffRule rule) {
        List<DiffRule> rules = findByMigrationKey(rule.getMigrationKey());
        rules.removeIf(r -> r.getRuleId().equals(rule.getRuleId()));
        rules.add(rule);
        publishRules(rule.getMigrationKey(), rules);
        return rule;
    }

    @Override
    public List<DiffRule> findByMigrationKey(String migrationKey) {
        Optional<String> contentOpt = configCenterGateway.getConfig(DATA_ID_PREFIX + migrationKey, GROUP);
        if (contentOpt.isEmpty() || contentOpt.get().isBlank()) {
            return new ArrayList<>();
        }
        try {
            List<DiffRuleConfig> configs = objectMapper.readValue(contentOpt.get(), new TypeReference<List<DiffRuleConfig>>() {});
            return configs.stream().map(this::toEntity).collect(Collectors.toList());
        } catch (Exception ex) {
            log.error("Failed to deserialize diff rules for migration key: {}", migrationKey, ex);
            return new ArrayList<>();
        }
    }

    @Override
    public void deleteByRuleId(String migrationKey, String ruleId) {
        List<DiffRule> rules = findByMigrationKey(migrationKey);
        boolean removed = rules.removeIf(r -> r.getRuleId().equals(ruleId));
        if (removed) {
            publishRules(migrationKey, rules);
        }
    }

    @Override
    public void deleteByMigrationKey(String migrationKey) {
        configCenterGateway.delete(DATA_ID_PREFIX + migrationKey, GROUP);
    }

    private void publishRules(String migrationKey, List<DiffRule> rules) {
        try {
            List<DiffRuleConfig> configs = rules.stream().map(this::toConfig).collect(Collectors.toList());
            String content = objectMapper.writeValueAsString(configs);
            configCenterGateway.publish(DATA_ID_PREFIX + migrationKey, GROUP, content);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to publish diff rules for migration key: " + migrationKey, ex);
        }
    }

    private DiffRule toEntity(DiffRuleConfig config) {
        return new DiffRule(
                config.migrationKey(),
                config.ruleId(),
                DiffRuleType.fromValue(config.ruleType()),
                config.fieldPath(),
                config.ruleValue(),
                config.enable(),
                config.createTime() != null ? config.createTime() : LocalDateTime.now(),
                config.updateTime() != null ? config.updateTime() : LocalDateTime.now()
        );
    }

    private DiffRuleConfig toConfig(DiffRule rule) {
        return new DiffRuleConfig(
                rule.getMigrationKey(),
                rule.getRuleId(),
                rule.getRuleType().name(),
                rule.getFieldPath(),
                rule.getRuleValue(),
                rule.isEnable(),
                rule.getCreateTime(),
                rule.getUpdateTime()
        );
    }

    private record DiffRuleConfig(
            @JsonProperty("migration_key") String migrationKey,
            @JsonProperty("rule_id") String ruleId,
            @JsonProperty("rule_type") String ruleType,
            @JsonProperty("field_path") String fieldPath,
            @JsonProperty("rule_value") String ruleValue,
            @JsonProperty("enable") boolean enable,
            @JsonProperty("create_time") LocalDateTime createTime,
            @JsonProperty("update_time") LocalDateTime updateTime) {
    }
}
