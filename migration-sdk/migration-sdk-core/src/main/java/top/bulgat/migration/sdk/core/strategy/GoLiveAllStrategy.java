package top.bulgat.migration.sdk.core.strategy;

import java.util.Map;
import top.bulgat.migration.sdk.core.model.MigrationTaskStatus;

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
    public MigrationTaskStatus getStatus() {
        return MigrationTaskStatus.GO_LIVE_ALL;
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

        // 并发调用，主线程执行新接口，辅助接口在后台跑
        ConcurrentInvocationResult<T> result = invokeNewMainOldAsync(context, grayParam);

        // 如果新接口执行成功，直接返回（后台在异步处理对比）
        if (result.newResult().isSuccess()) {
            return result.newResult().value();
        }

        // 发生异常时，如果是默认的降级逻辑且需要旧接口结果同步兜底
        // 这里采用重新调用旧接口的方式，保证主线程工具方法内零阻塞
        return executeFallback(context, result.newResult().error());
    }
}
