package top.bulgat.migration.sdk.core.strategy;

import java.util.Map;
import top.bulgat.migration.sdk.core.model.MigrationStatus;

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
    public MigrationStatus getStatus() {
        return MigrationStatus.VALIDATION_GRAY;
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
        Map<String, Object> grayscaleParam = context.buildParam();
        boolean hitGray = matchGrayscale(context, grayscaleParam);

        // 并发执行旧接口和新接口
        ConcurrentInvocationResult<T> result = invokeOldMainNewAsync(context);

        // 如果命中灰度，则异步发送Diff对比
        if (hitGray) {
            sendDiffAsync(context, result.oldResult(), result.newResult(), grayscaleParam);
        }

        // 始终优先返回旧接口的结果
        if (result.oldResult().isSuccess()) {
            return result.oldResult().value();
        }

        // 旧接口失败时，执行降级逻辑
        return executeFallbackAfterOldFailed(context, result.oldResult().error());
    }
}
