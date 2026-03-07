package top.bulgat.migration.diff.infrastructure.configcenter;

import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.config.ConfigService;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;
import top.bulgat.migration.diff.domain.model.DiffRule;
import top.bulgat.migration.diff.domain.model.DiffRuleType;
import top.bulgat.migration.diff.domain.repository.DiffRuleRepository;

/**
 * NacosDiffRuleRepository 定义持久化访问能力。
 */
@Primary
@Repository
public class NacosDiffRuleRepository implements DiffRuleRepository {

    private static final Logger log = LoggerFactory.getLogger(NacosDiffRuleRepository.class);
    private static final String RULE_DATA_ID_PREFIX = "migration_";
    private static final String LEGACY_RULE_DATA_ID_PREFIX = "diff_";
    private static final String RULE_GROUP = "DIFF_RULE_GROUP";
    private static final String DEFAULT_GROUP = "DEFAULT_GROUP";

    private final ConfigService configService;
    private final ObjectMapper objectMapper;

    @org.springframework.beans.factory.annotation.Autowired
    public NacosDiffRuleRepository(
            ObjectMapper objectMapper,
            @Value("${migration.nacos.server-addr:localhost:8848}") String serverAddr,
            @Value("${migration.nacos.namespace:}") String namespace,
            @Value("${migration.nacos.username:}") String username,
            @Value("${migration.nacos.password:}") String password) throws Exception {
        this.objectMapper = objectMapper;
        Properties properties = new Properties();
        properties.setProperty("serverAddr", serverAddr);
        if (namespace != null && !namespace.isBlank()) {
            properties.setProperty("namespace", namespace);
        }
        if (username != null && !username.isBlank()) {
            properties.setProperty("username", username);
        }
        if (password != null && !password.isBlank()) {
            properties.setProperty("password", password);
        }
        this.configService = NacosFactory.createConfigService(properties);
    }

    NacosDiffRuleRepository(ObjectMapper objectMapper, ConfigService configService) {
        this.objectMapper = objectMapper;
        this.configService = configService;
    }

    /**
     * 执行 findEnabledRules 业务逻辑。
     * @param migrationKey 迁移标识。
     * @return 返回结果。
     */
    @Override
    public List<DiffRule> findEnabledRules(String migrationKey) {
        try {
            String content = configService.getConfig(RULE_DATA_ID_PREFIX + migrationKey, RULE_GROUP, 3000);
            if (content == null || content.isBlank()) {
                content = configService.getConfig(RULE_DATA_ID_PREFIX + migrationKey, DEFAULT_GROUP, 3000);
            }
            if (content == null || content.isBlank()) {
                content = configService.getConfig(LEGACY_RULE_DATA_ID_PREFIX + migrationKey, DEFAULT_GROUP, 3000);
            }
            if (content == null || content.isBlank()) {
                return List.of();
            }
            List<RuleConfig> configs = objectMapper.readValue(content, new TypeReference<List<RuleConfig>>() {
            });
            if (configs == null || configs.isEmpty()) {
                return List.of();
            }
            List<DiffRule> rules = new ArrayList<>();
            for (RuleConfig config : configs) {
                if (config == null || !config.enable()) {
                    continue;
                }
                try {
                    String ruleMigrationKey = config.migrationKey();
                    if (ruleMigrationKey == null || ruleMigrationKey.isBlank()) {
                        ruleMigrationKey = migrationKey;
                    }
                    rules.add(new DiffRule(
                            ruleMigrationKey,
                            DiffRuleType.fromValue(config.ruleType()),
                            config.fieldPath(),
                            config.ruleValue(),
                            true));
                } catch (IllegalArgumentException ex) {
                    log.warn(
                            "skip invalid diff rule from nacos, migrationKey={}, ruleType={}, fieldPath={}, reason={}",
                            migrationKey,
                            config.ruleType(),
                            config.fieldPath(),
                            sanitizeReason(ex));
                    log.debug("invalid diff rule detail, migrationKey={}", migrationKey, ex);
                }
            }
            return rules;
        } catch (Exception ex) {
            log.warn(
                    "failed to load diff rules from nacos, fallback to empty, migrationKey={}, reason={}",
                    migrationKey,
                    sanitizeReason(ex));
            log.debug("failed to load diff rules detail, migrationKey={}", migrationKey, ex);
            return List.of();
        }
    }

    private String sanitizeReason(Exception ex) {
        if (ex == null || ex.getMessage() == null) {
            return "unknown";
        }
        return ex.getMessage().replaceAll("\\s+", " ").trim();
    }

    private record RuleConfig(
            @JsonProperty("migration_key") String migrationKey,
            @JsonProperty("rule_type") String ruleType,
            @JsonProperty("field_path") String fieldPath,
            @JsonProperty("rule_value") String ruleValue,
            @JsonProperty("enable") boolean enable) {
    }
}
