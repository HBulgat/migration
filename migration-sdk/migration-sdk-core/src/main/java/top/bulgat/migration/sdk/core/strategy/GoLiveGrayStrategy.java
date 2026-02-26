package top.bulgat.migration.sdk.core.strategy;

import java.util.Map;
import top.bulgat.migration.sdk.core.model.MigrationStatus;
import top.bulgat.migration.sdk.core.support.InvocationResult;

/**
 * Stage 4 (GO_LIVE_GRAY): return new result when grayscale hit, otherwise return old result.
 */
public class GoLiveGrayStrategy extends AbstractMigrationStrategy {

    /**
     * Returns the migration status handled by this strategy.
     *
     * @return target migration status
     */
    @Override
    public MigrationStatus getStatus() {
        return MigrationStatus.GO_LIVE_GRAY;
    }

    /**
     * Executes routing logic for the current migration stage.
     *
     * @param context execution context
     * @param <T> return type
     * @return routed execution result
     */
    @Override
    public <T> T execute(MigrationExecutionContext<T> context) {
        Map<String, Object> grayscaleParam = context.buildParam();
        boolean hitGray = matchGrayscale(context, grayscaleParam);

        if (hitGray) {
            InvocationResult<T> newResult = invokeSafely(context.getNewMethod(), context.getArgs());
            if (newResult.isSuccess()) {
                return newResult.value();
            }
            return executeFallback(context, newResult.error());
        }

        ConcurrentInvocationResult<T> concurrentResult = invokeOldMainNewAsync(context);
        sendDiffAsync(context, concurrentResult.oldResult(), concurrentResult.newResult(), grayscaleParam);
        if (concurrentResult.oldResult().isSuccess()) {
            return concurrentResult.oldResult().value();
        }
        return executeFallbackAfterOldFailed(context, concurrentResult.oldResult().error());
    }
}
