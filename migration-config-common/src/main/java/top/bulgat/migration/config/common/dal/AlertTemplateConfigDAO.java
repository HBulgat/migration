package top.bulgat.migration.config.common.dal;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.ibatis.annotations.Mapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.bulgat.common.base.util.JsonUtils;
import top.bulgat.common.base.util.StringUtils;
import top.bulgat.migration.config.common.configcenter.ConfigCenterGateway;
import top.bulgat.migration.config.common.model.dataobject.AlertTemplateConfig;

import java.util.HashMap;
import java.util.Map;

@Mapper
public class AlertTemplateConfigDAO {

    private static final String DATA_ID = "alert_template";
    private static final String GROUP = ConfigCenterGateway.DEFAULT_GROUP;

    private final ConfigCenterGateway configCenterGateway;

    public AlertTemplateConfigDAO(ConfigCenterGateway configCenterGateway) {
        this.configCenterGateway = configCenterGateway;
    }

    /**
     * 读取所有的 Alert 模板规则映射。
     *
     * @return 规则 DO map，若不存在则返回空 map
     */
    public Map<String, AlertTemplateConfig> findAll() {
        var content = configCenterGateway.getConfig(DATA_ID, GROUP);
        if (content.isEmpty() || StringUtils.isBlank(content.get())) {
            return new HashMap<>();
        }
        Map<String,AlertTemplateConfig> configMap = JsonUtils.toMap(content.get(),AlertTemplateConfig.class);
        return configMap == null ? new HashMap<>() : configMap;
    }

    /**
     * 保存 Alert 模板列表到配置中心。
     *
     * @param configs 模板规则 DO 映射
     */
    public void save(Map<String,AlertTemplateConfig> configs) {
        try {
            String content = JsonUtils.toJson(configs);
            configCenterGateway.publish(DATA_ID, GROUP, content);
        } catch (Exception ex) {
            throw new IllegalStateException("failed to publish alert templates", ex);
        }
    }
}
