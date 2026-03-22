package top.bulgat.migration.sdk.core.strategy;

import java.util.Map;
import top.bulgat.migration.sdk.core.model.MigrationTaskStatus;

/**
 * 第3阶段（验证-全开）：并发调用新旧接口比对，但始终返回旧接口结果。
 */
public class ValidationAllStrategy extends AbstractMigrationStrategy {

    /**
     * 返回当前策略处理的迁移状态。
     *
     * @return 目标迁移状态
     */
    @Override
    public MigrationTaskStatus getStatus() {
        return MigrationTaskStatus.VALIDATION_ALL;
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
        Map<String, Object> grayParam = context.buildParam();

        // 并发执行旧接口和新接口，后台异步上报 Diff
        ConcurrentInvocationResult<T> result = invokeOldMainNewAsync(context, grayParam);

        // 返回旧接口结果
        if (result.oldResult().isSuccess()) {
            return result.oldResult().value();
        }

        // 旧接口失败时，执行降级逻辑
        return executeFallbackAfterOldFailed(context, result.oldResult().error());
    }
}
