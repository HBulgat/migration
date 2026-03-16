package top.bulgat.migration.sdk.core.strategy;

import java.util.Map;
import top.bulgat.migration.sdk.core.model.MigrationTaskStatus;
import top.bulgat.migration.sdk.core.support.InvocationResult;

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
            InvocationResult<T> newResult = invokeSafely(context.getNewMethod(), context.getArgs());
            if (newResult.isSuccess()) {
                return newResult.value();
            }
            return executeFallback(context, newResult.error());
        }

        // 未命中灰度，并发调用，主线程执行新接口
        ConcurrentInvocationResult<T> result = invokeNewMainOldAsync(context, grayscaleParam);

        if (result.newResult().isSuccess()) {
            return result.newResult().value();
        }

        // 发生异常时触发降级流程
        return executeFallback(context, result.newResult().error());
    }
}
