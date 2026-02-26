package top.bulgat.migration.sdk.core.strategy;

import java.util.Map;
import top.bulgat.migration.sdk.core.model.MigrationStatus;

/**
 * Stage 2 (VALIDATION_GRAY): call old/new concurrently and return old result.
 */
public class ValidationGrayStrategy extends AbstractMigrationStrategy {

    /**
     * Returns the migration status handled by this strategy.
     *
     * @return target migration status
     */
    @Override
    public MigrationStatus getStatus() {
        return MigrationStatus.VALIDATION_GRAY;
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

        ConcurrentInvocationResult<T> result = invokeOldMainNewAsync(context);
        if (hitGray) {
             sendDiffAsync(context, result.oldResult(), result.newResult(), grayscaleParam);
        }
        if (result.oldResult().isSuccess()) {
            return result.oldResult().value();
        }
        return executeFallbackAfterOldFailed(context, result.oldResult().error());
    }
}
