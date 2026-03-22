package top.bulgat.migration.sdk.core.strategy;

import java.util.Map;
import top.bulgat.migration.sdk.core.model.MigrationTaskStatus;
import top.bulgat.migration.sdk.core.support.InvocationResult;

/**
 * 第4阶段（上线-灰度）：命中灰度时调用并返回新接口结果，未命中时并发调用并返回旧接口结果。
 */
public class GoLiveGrayStrategy extends AbstractMigrationStrategy {

    /**
     * 返回当前策略处理的迁移状态。
     *
     * @return 目标迁移状态
     */
    @Override
    public MigrationTaskStatus getStatus() {
        return MigrationTaskStatus.GO_LIVE_GRAY;
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

        if (hitGray) {
            // 命中灰度：仅调用新接口
            InvocationResult<T> newResult = invokeSafely(context.getNewMethod(), context.getArgs());
            if (newResult.isSuccess()) {
                return newResult.value();
            }
            // 新接口出现异常时执行降级逻辑
            return executeFallback(context, newResult.error());
        }

        // 未命中灰度：并发执行并异步发送Diff，并返回旧接口结果
        ConcurrentInvocationResult<T> concurrentResult = invokeOldMainNewAsync(context, grayParam);

        if (concurrentResult.oldResult().isSuccess()) {
            return concurrentResult.oldResult().value();
        }

        // 旧接口执行失败时，调用降级方法
        return executeFallbackAfterOldFailed(context, concurrentResult.oldResult().error());
    }
}
