package top.bulgat.migration.diff.domain.repository;

import java.util.List;
import top.bulgat.migration.diff.domain.model.AlertRule;

/**
 * 告警规则仓储接口。
 */
public interface AlertRuleRepository {

    /**
     * 查找指定 migrationKey 下所有启用的告警规则。
     *
     * @param migrationKey 迁移任务标识
     * @return 启用的告警规则列表
     */
    List<AlertRule> findEnabledRules(String migrationKey);
}
