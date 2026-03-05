package top.bulgat.migration.sdk.core.strategy;

import java.util.Map;
import top.bulgat.migration.sdk.core.model.MigrationStatus;
import top.bulgat.migration.sdk.core.support.OldInvocationFailedException;

/**
 * 第5阶段（上线-全开）：并发调用新旧接口比对，返回新接口结果。
 */
public class GoLiveAllStrategy extends AbstractMigrationStrategy {

    /**
     * 返回当前策略处理的迁移状态。
     *
     * @return 目标迁移状态
     */
    @Override
    public MigrationStatus getStatus() {
        return MigrationStatus.GO_LIVE_ALL;
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
        Map<String, Object> grayscaleParam = context.buildParam();

        // 并发调用，主线程执行新接口
        ConcurrentInvocationResult<T> result = invokeNewMainOldAsync(context);
        sendDiffAsync(context, result.oldResult(), result.newResult(), grayscaleParam, true, false);

        // 返回新接口结果
        if (result.newResult().isSuccess()) {
            return result.newResult().value();
        }

        // 发生异常时，如果是默认的降级逻辑且旧接口成功了，用旧接口结果兜底
        if (context.isDefaultOldFallback()) {
            if (result.oldResult().isSuccess()) {
                return result.oldResult().value();
            }
            throw new OldInvocationFailedException(result.oldResult().error());
        }

        return executeFallback(context, result.newResult().error());
    }
}
