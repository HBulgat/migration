package top.bulgat.migration.config.common.dal;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.ibatis.annotations.Mapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.bulgat.common.base.util.JsonUtils;
import top.bulgat.common.base.util.StringUtils;
import top.bulgat.migration.config.common.configcenter.ConfigCenterGateway;
import top.bulgat.migration.config.common.model.dataobject.AlertRuleConfig;

import java.util.ArrayList;
import java.util.List;

@Mapper
public class AlertRuleConfigDAO {

    private static final String DATA_ID_PREFIX = "alert_rule_";
    private static final String GROUP = ConfigCenterGateway.DEFAULT_GROUP;

    private final ConfigCenterGateway configCenterGateway;

    public AlertRuleConfigDAO(ConfigCenterGateway configCenterGateway) {
        this.configCenterGateway = configCenterGateway;
    }

    /**
     * 读取指定 migrationKey 下的所有 Alert 规则。
     *
     * @param migrationKey 迁移标识
     * @return 规则 DO 列表，异常时返回空列表
     */
    public List<AlertRuleConfig> findByMigrationKey(String migrationKey) {
        var content = configCenterGateway.getConfig(DATA_ID_PREFIX + migrationKey, GROUP);
        if (content.isEmpty() || StringUtils.isBlank(content.get())) {
            return new ArrayList<>();
        }
        List<AlertRuleConfig> configs = JsonUtils.toList(content.get(), AlertRuleConfig.class);
        return configs != null ? new ArrayList<>(configs) : new ArrayList<>();
    }

    /**
     * 保存 Alert 规则列表到配置中心。
     *
     * @param migrationKey 迁移标识
     * @param configs      规则 DO 列表
     */
    public void save(String migrationKey, List<AlertRuleConfig> configs) {
        String content = JsonUtils.toJson(configs);
        configCenterGateway.publish(DATA_ID_PREFIX + migrationKey, GROUP, content);
    }

    /**
     * 删除指定 migrationKey 的 Diff 规则配置。
     *
     * @param migrationKey 迁移标识
     */
    public void delete(String migrationKey) {
        configCenterGateway.delete(DATA_ID_PREFIX + migrationKey, GROUP);
    }
}
