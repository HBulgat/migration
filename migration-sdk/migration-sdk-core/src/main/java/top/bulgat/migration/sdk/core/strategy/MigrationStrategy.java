package top.bulgat.migration.sdk.core.strategy;

import top.bulgat.migration.sdk.core.model.MigrationStatus;

/**
 * 迁移策略接口：每个迁移状态对应一个策略实现。
 */
public interface MigrationStrategy {

    /**
     * 当前策略支持的迁移状态。
     *
     * @return 迁移状态
     */
    MigrationStatus getStatus();

    /**
     * 执行迁移策略。
     *
     * @param context 执行上下文
     * @param <T> 返回值类型
     * @return 执行结果
     */
    <T> T execute(MigrationExecutionContext<T> context);
}
