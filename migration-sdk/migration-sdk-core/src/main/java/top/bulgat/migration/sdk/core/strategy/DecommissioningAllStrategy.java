package top.bulgat.migration.sdk.core.strategy;

import top.bulgat.migration.sdk.core.model.MigrationStatus;

/**
 * 第7阶段（停用-全开）：仅调用新接口方法。
 */
public class DecommissioningAllStrategy extends AbstractMigrationStrategy {

    /**
     * 返回当前策略处理的迁移状态。
     *
     * @return 目标迁移状态
     */
    @Override
    public MigrationStatus getStatus() {
        return MigrationStatus.DECOMMISSIONING_ALL;
    }

    /**
     * 执行当前迁移阶段的路由逻辑。
     *
     * @param context 执行上下文
     * @param <T>     返回值类型
     * @return 路由执行结果
     */
    @Override
    public <T> T execute(MigrationExecutionContext<T> context) {
        var newResult = invokeSafely(context.getNewMethod(), context.getArgs());
        if (newResult.isSuccess()) {
            return newResult.value();
        }
        return executeFallback(context, newResult.error());
    }
}
