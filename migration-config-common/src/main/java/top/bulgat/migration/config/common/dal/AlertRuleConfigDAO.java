package top.bulgat.migration.config.common.dal;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.ibatis.annotations.Mapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.bulgat.migration.config.common.configcenter.ConfigCenterGateway;
import top.bulgat.migration.config.common.model.dataobject.AlertRuleConfig;

import java.util.ArrayList;
import java.util.List;

@Mapper
public class AlertRuleConfigDAO {

    private static final Logger log = LoggerFactory.getLogger(AlertRuleConfigDAO.class);
    private static final String DATA_ID_PREFIX = "alert_rule_";
    private static final String GROUP = ConfigCenterGateway.DEFAULT_GROUP;

    private final ConfigCenterGateway configCenterGateway;
    private final ObjectMapper objectMapper;

    public AlertRuleConfigDAO(ConfigCenterGateway configCenterGateway, ObjectMapper objectMapper) {
        this.configCenterGateway = configCenterGateway;
        this.objectMapper = objectMapper;
    }

    /**
     * 读取指定 migrationKey 下的所有 Alert 规则。
     *
     * @param migrationKey 迁移标识
     * @return 规则 DO 列表，异常时返回空列表
     */
    public List<AlertRuleConfig> findByMigrationKey(String migrationKey) {
        try {
            var content = configCenterGateway.getConfig(DATA_ID_PREFIX + migrationKey, GROUP);
            if (content.isEmpty() || content.get().isBlank()) {
                return new ArrayList<>();
            }
            List<AlertRuleConfig> configs = objectMapper.readValue(content.get(), new TypeReference<>() {});
            return configs != null ? new ArrayList<>(configs) : new ArrayList<>();
        } catch (Exception ex) {
            log.warn("failed to load alert rules from config center, migrationKey={}, reason={}",
                    migrationKey, sanitizeReason(ex));
            log.debug("failed to load alert rules detail, migrationKey={}", migrationKey, ex);
            return new ArrayList<>();
        }
    }

    /**
     * 保存 Alert 规则列表到配置中心。
     *
     * @param migrationKey 迁移标识
     * @param configs      规则 DO 列表
     */
    public void save(String migrationKey, List<AlertRuleConfig> configs) {
        try {
            String content = objectMapper.writeValueAsString(configs);
            configCenterGateway.publish(DATA_ID_PREFIX + migrationKey, GROUP, content);
        } catch (Exception ex) {
            throw new IllegalStateException(
                    "failed to publish alert rules for migrationKey: " + migrationKey, ex);
        }
    }

    /**
     * 删除指定 migrationKey 的 Diff 规则配置。
     *
     * @param migrationKey 迁移标识
     */
    public void delete(String migrationKey) {
        configCenterGateway.delete(DATA_ID_PREFIX + migrationKey, GROUP);
    }

    private String sanitizeReason(Exception ex) {
        if (ex == null || ex.getMessage() == null) {
            return "unknown";
        }
        return ex.getMessage().replaceAll("\\s+", " ").trim();
    }
}
