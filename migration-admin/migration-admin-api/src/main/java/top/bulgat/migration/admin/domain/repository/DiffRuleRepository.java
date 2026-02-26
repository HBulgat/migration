package top.bulgat.migration.admin.domain.repository;

import java.util.List;
import top.bulgat.migration.admin.domain.model.DiffRule;

/**
 * Diff规则仓储接口。
 */
public interface DiffRuleRepository {

    /**
     * 保存Diff规则
     *
     * @param rule 规则对象
     * @return 规则对象
     */
    DiffRule save(DiffRule rule);

    /**
     * 根据migrationKey查询所有Diff规则
     *
     * @param migrationKey 迁移任务key
     * @return 规则列表
     */
    List<DiffRule> findByMigrationKey(String migrationKey);

    /**
     * 删除指定规则
     *
     * @param migrationKey 迁移任务key
     * @param ruleId 规则ID
     */
    void deleteByRuleId(String migrationKey, String ruleId);

    /**
     * 删除指定任务下的所有规则
     *
     * @param migrationKey 迁移任务key
     */
    void deleteByMigrationKey(String migrationKey);
}
