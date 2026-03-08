package top.bulgat.migration.sdk.core.strategy;

import java.util.Map;
import top.bulgat.migration.sdk.core.model.MigrationTaskStatus;
import top.bulgat.migration.sdk.core.support.OldInvocationFailedException;

/**
 * 第6阶段（停用-灰度）：命中灰度时仅调用新接口，未命中则并发调用新旧接口并执行Diff，始终优先返回新接口结果。
 */
public class DecommissioningGrayStrategy extends AbstractMigrationStrategy {

    /**
     * 返回当前策略处理的迁移状态。
     *
     * @return 目标迁移状态
     */
    @Override
    public MigrationTaskStatus getStatus() {
        return MigrationTaskStatus.DECOMMISSIONING_GRAY;
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
        boolean hitGray = matchGrayscale(context, grayscaleParam);

        if (hitGray) {
            // 命中灰度，仅调用新接口
            var newResult = invokeSafely(context.getNewMethod(), context.getArgs());
            if (newResult.isSuccess()) {
                return newResult.value();
            }
            return executeFallback(context, newResult.error());
        }

        // 未命中灰度，并发调用，主线程执行新接口，异步线程执行旧接口
        ConcurrentInvocationResult<T> result = invokeNewMainOldAsync(context);
        sendDiffAsync(context, result.oldResult(), result.newResult(), grayscaleParam, false, false);

        if (result.newResult().isSuccess()) {
            return result.newResult().value();
        }

        // 发生异常时，如果是默认的降级逻辑，而且旧接口成功了，用旧接口结果兜底
        if (context.isDefaultOldFallback()) {
            if (result.oldResult().isSuccess()) {
                return result.oldResult().value();
            }
            // 抛出特定的标志异常避免重复调用
            throw new OldInvocationFailedException(result.oldResult().error());
        }

        return executeFallback(context, result.newResult().error());
    }
}
