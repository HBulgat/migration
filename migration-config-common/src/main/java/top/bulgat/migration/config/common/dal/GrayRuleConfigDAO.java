package top.bulgat.migration.config.common.dal;

import java.util.ArrayList;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import top.bulgat.common.base.util.JsonUtils;
import top.bulgat.migration.config.common.configcenter.ConfigCenterGateway;
import top.bulgat.migration.config.common.model.dataobject.GrayRuleConfig;

/**
 * 灰度规则配置中心数据访问对象。
 * <p>
 * 统一封装配置中心的读写操作和 DO 序列化/反序列化，供 admin-api 与 diff-service 共用。
 */
@Mapper
public class GrayRuleConfigDAO {

    private static final String DATA_ID_PREFIX = "gray_rule_";
    private static final String GROUP = ConfigCenterGateway.DEFAULT_GROUP;

    private final ConfigCenterGateway configCenterGateway;

    public GrayRuleConfigDAO(ConfigCenterGateway configCenterGateway) {
        this.configCenterGateway = configCenterGateway;
    }

    /**
     * 读取指定 migrationKey 下的所有灰度规则。
     *
     * @param migrationKey 迁移标识
     * @return 规则 DO 列表，异常时抛出 IllegalStateException
     */
    public List<GrayRuleConfig> findByMigrationKey(String migrationKey) {
        var content = configCenterGateway.getConfig(DATA_ID_PREFIX + migrationKey, GROUP);
        if (content.isEmpty() || content.get().isBlank()) {
            return new ArrayList<>();
        }
        List<GrayRuleConfig> configs = JsonUtils.toList(content.get(), GrayRuleConfig.class);
        return configs != null ? new ArrayList<>(configs) : new ArrayList<>();
    }

    /**
     * 保存灰度规则列表到配置中心。
     *
     * @param migrationKey 迁移标识
     * @param configs      规则 DO 列表
     */
    public void save(String migrationKey, List<GrayRuleConfig> configs) {
        String content = JsonUtils.toJson(configs);
        configCenterGateway.publish(DATA_ID_PREFIX + migrationKey, GROUP, content);
    }

    /**
     * 删除指定 migrationKey 的灰度规则配置。
     *
     * @param migrationKey 迁移标识
     */
    public void delete(String migrationKey) {
        configCenterGateway.delete(DATA_ID_PREFIX + migrationKey, GROUP);
    }
}
