package top.bulgat.migration.sdk.core.strategy;

import top.bulgat.migration.sdk.core.model.MigrationStatus;
import top.bulgat.migration.sdk.core.support.InvocationResult;

/**
 * Stage 1 (OLD): only call old method.
 */
public class OldOnlyStrategy extends AbstractMigrationStrategy {

    /**
     * Returns the migration status handled by this strategy.
     *
     * @return target migration status
     */
    @Override
    public MigrationStatus getStatus() {
        return MigrationStatus.OLD;
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
        InvocationResult<T> oldResult = invokeSafely(context.getOldMethod(), context.getArgs());
        if (oldResult.isSuccess()) {
            return oldResult.value();
        }
        return executeFallbackAfterOldFailed(context, oldResult.error());
    }
}
