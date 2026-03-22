package top.bulgat.migration.sdk.core.strategy;

import java.util.Map;
import top.bulgat.migration.sdk.core.model.MigrationTaskStatus;
import top.bulgat.migration.sdk.core.support.InvocationResult;

/**
 * 第2阶段（验证-灰度）：并发调用新旧接口，执行Diff比对，但始终返回旧接口结果。
 */
public class ValidationGrayStrategy extends AbstractMigrationStrategy {

    /**
     * 返回当前策略处理的迁移状态。
     *
     * @return 目标迁移状态
     */
    @Override
    public MigrationTaskStatus getStatus() {
        return MigrationTaskStatus.VALIDATION_GRAY;
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
        // 获取灰度参数并进行匹配
        Map<String, Object> grayParam = context.buildParam();
        boolean hitGray = matchGray(context, grayParam);

        // 如果命中灰度，并发执行并异步发送Diff对比；否则仅执行旧接口
        if (hitGray) {
            ConcurrentInvocationResult<T> result = invokeOldMainNewAsync(context, grayParam);
            if (result.oldResult().isSuccess()) {
                return result.oldResult().value();
            }
            return executeFallbackAfterOldFailed(context, result.oldResult().error());
        }

        // 未命中灰度，仅调用旧接口（不执行并发对比）
        InvocationResult<T> oldResult = invokeSafely(context.getOldMethod(), context.getArgs());
        if (oldResult.isSuccess()) {
            return oldResult.value();
        }
        return executeFallbackAfterOldFailed(context, oldResult.error());
    }
}
