package top.bulgat.migration.sdk.core.strategy;

import java.util.Map;
import top.bulgat.migration.sdk.core.model.MigrationStatus;
import top.bulgat.migration.sdk.core.support.OldInvocationFailedException;

/**
 * Stage 6 (DECOMMISSIONING_GRAY): return new result, with grayscale-only fast path.
 */
public class DecommissioningGrayStrategy extends AbstractMigrationStrategy {

    /**
     * Returns the migration status handled by this strategy.
     *
     * @return target migration status
     */
    @Override
    public MigrationStatus getStatus() {
        return MigrationStatus.DECOMMISSIONING_GRAY;
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
            var newResult = invokeSafely(context.getNewMethod(), context.getArgs());
            if (newResult.isSuccess()) {
                return newResult.value();
            }
            return executeFallback(context, newResult.error());
        }

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
