package top.bulgat.migration.config.common.dal;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.ibatis.annotations.Mapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.bulgat.migration.config.common.configcenter.ConfigCenterGateway;
import top.bulgat.migration.config.common.model.dataobject.AlertTemplateConfig;

import java.util.HashMap;
import java.util.Map;

@Mapper
public class AlertTemplateConfigDAO {

    private static final Logger log = LoggerFactory.getLogger(AlertTemplateConfigDAO.class);
    private static final String DATA_ID = "alert_template";
    private static final String GROUP = ConfigCenterGateway.DEFAULT_GROUP;

    private final ConfigCenterGateway configCenterGateway;
    private final ObjectMapper objectMapper;

    public AlertTemplateConfigDAO(ConfigCenterGateway configCenterGateway, ObjectMapper objectMapper) {
        this.configCenterGateway = configCenterGateway;
        this.objectMapper = objectMapper;
    }

//    /**
//     * 读取指定 templateKey 下的 Alert 规则。
//     *
//     * @param templateKey 迁移标识
//     * @return 规则 DO 列表，异常时返回null
//     */
//    public AlertTemplateConfig findByTemplateKey(String templateKey) {
//        try {
//            var content = configCenterGateway.getConfig(DATA_ID, GROUP);
//            if (content.isEmpty() || content.get().isBlank()) {
//                return null;
//            }
//            Map<String,AlertTemplateConfig> configMap = objectMapper.readValue(content.get(), new TypeReference<>() {});
//            return configMap == null ? null : configMap.getOrDefault(templateKey,null);
//        } catch (Exception ex) {
//            log.warn("failed to load alert templates from config center, templateKey={}, reason={}",
//                    templateKey, sanitizeReason(ex));
//            log.debug("failed to load alert templates detail, templateKey={}", templateKey, ex);
//            return null;
//        }
//    }

    /**
     * 读取指定 所有的 Alert 规则。
     *
     * @return 规则 DO map，异常时返回空map
     */
    public Map<String,AlertTemplateConfig> findAll() {
        try {
            var content = configCenterGateway.getConfig(DATA_ID, GROUP);
            if (content.isEmpty() || content.get().isBlank()) {
                return null;
            }
            Map<String,AlertTemplateConfig> configMap = objectMapper.readValue(content.get(), new TypeReference<>() {});
            return configMap == null ? new HashMap<>() : configMap;
        } catch (Exception ex) {
            log.warn("failed to load alert templates from config center, reason={}", sanitizeReason(ex));
            log.debug("failed to load alert template detail", ex);
            return null;
        }
    }

    /**
     * 保存 Alert 规则列表到配置中心。
     *
     * @param migrationKey 迁移标识
     * @param configs      规则 DO 列表
     */
    public void save(String migrationKey, Map<String,AlertTemplateConfig> configs) {
        try {
            String content = objectMapper.writeValueAsString(configs);
            configCenterGateway.publish(DATA_ID + migrationKey, GROUP, content);
        } catch (Exception ex) {
            throw new IllegalStateException("failed to publish alert templates", ex);
        }
    }

    private String sanitizeReason(Exception ex) {
        if (ex == null || ex.getMessage() == null) {
            return "unknown";
        }
        return ex.getMessage().replaceAll("\\s+", " ").trim();
    }
}
