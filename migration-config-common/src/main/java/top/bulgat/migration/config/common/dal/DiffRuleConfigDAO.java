package top.bulgat.migration.config.common.dal;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.bulgat.migration.config.common.configcenter.ConfigCenterGateway;
import top.bulgat.migration.config.common.model.dataobject.DiffRuleConfig;

/**
 * Diff 规则配置中心数据访问对象。
 * <p>
 * 统一封装配置中心的读写操作和 DO 序列化/反序列化，供 admin-api 与 diff-service 共用。
 */
@Mapper
public class DiffRuleConfigDAO {

    private static final Logger log = LoggerFactory.getLogger(DiffRuleConfigDAO.class);
    private static final String DATA_ID_PREFIX = "diff_rule_";
    private static final String GROUP = ConfigCenterGateway.DEFAULT_GROUP;

    private final ConfigCenterGateway configCenterGateway;
    private final ObjectMapper objectMapper;

    public DiffRuleConfigDAO(ConfigCenterGateway configCenterGateway, ObjectMapper objectMapper) {
        this.configCenterGateway = configCenterGateway;
        this.objectMapper = objectMapper;
    }

    /**
     * 读取指定 migrationKey 下的所有 Diff 规则。
     *
     * @param migrationKey 迁移标识
     * @return 规则 DO 列表，异常时返回空列表
     */
    public List<DiffRuleConfig> findByMigrationKey(String migrationKey) {
        try {
            var content = configCenterGateway.getConfig(DATA_ID_PREFIX + migrationKey, GROUP);
            if (content.isEmpty() || content.get().isBlank()) {
                return new ArrayList<>();
            }
            List<DiffRuleConfig> configs = objectMapper.readValue(content.get(), new TypeReference<>() {});
            return configs != null ? new ArrayList<>(configs) : new ArrayList<>();
        } catch (Exception ex) {
            log.warn("failed to load diff rules from config center, migrationKey={}, reason={}",
                    migrationKey, sanitizeReason(ex));
            log.debug("failed to load diff rules detail, migrationKey={}", migrationKey, ex);
            return new ArrayList<>();
        }
    }

    /**
     * 保存 Diff 规则列表到配置中心。
     *
     * @param migrationKey 迁移标识
     * @param configs      规则 DO 列表
     */
    public void save(String migrationKey, List<DiffRuleConfig> configs) {
        try {
            String content = objectMapper.writeValueAsString(configs);
            configCenterGateway.publish(DATA_ID_PREFIX + migrationKey, GROUP, content);
        } catch (Exception ex) {
            throw new IllegalStateException(
                    "failed to publish diff rules for migrationKey: " + migrationKey, ex);
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
