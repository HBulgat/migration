package top.bulgat.migration.sdk.core.strategy;

import top.bulgat.migration.sdk.core.model.MigrationTaskStatus;
import top.bulgat.migration.sdk.core.support.InvocationResult;

/**
 * 第1阶段（单旧）：仅调用旧接口方法。
 */
public class OldOnlyStrategy extends AbstractMigrationStrategy {

    /**
     * 返回当前策略处理的迁移状态。
     *
     * @return 目标迁移状态
     */
    @Override
    public MigrationTaskStatus getStatus() {
        return MigrationTaskStatus.OLD;
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
        InvocationResult<T> oldResult = invokeSafely(context.getOldMethod(), context.getArgs());
        if (oldResult.isSuccess()) {
            return oldResult.value();
        }
        return executeFallbackAfterOldFailed(context, oldResult.error());
    }
}
