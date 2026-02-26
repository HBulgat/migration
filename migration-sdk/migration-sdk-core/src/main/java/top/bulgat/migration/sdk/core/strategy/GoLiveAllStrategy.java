package top.bulgat.migration.sdk.core.strategy;

import java.util.Map;
import top.bulgat.migration.sdk.core.model.MigrationStatus;
import top.bulgat.migration.sdk.core.support.OldInvocationFailedException;

/**
 * Stage 5 (GO_LIVE_ALL): call old/new concurrently and return new result.
 */
public class GoLiveAllStrategy extends AbstractMigrationStrategy {

    /**
     * Returns the migration status handled by this strategy.
     *
     * @return target migration status
     */
    @Override
    public MigrationStatus getStatus() {
        return MigrationStatus.GO_LIVE_ALL;
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
        ConcurrentInvocationResult<T> result = invokeNewMainOldAsync(context);
        sendDiffAsync(context, result.oldResult(), result.newResult(), grayscaleParam);
        if (result.newResult().isSuccess()) {
            return result.newResult().value();
        }
        if (context.isDefaultOldFallback()) {
            if (result.oldResult().isSuccess()) {
                return result.oldResult().value();
            }
            throw new OldInvocationFailedException(result.oldResult().error());
        }
        return executeFallback(context, result.newResult().error());
    }
}
