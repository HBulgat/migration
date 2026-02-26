package top.bulgat.migration.sdk.core.strategy;

import top.bulgat.migration.sdk.core.model.MigrationStatus;

/**
 * Stage 7 (DECOMMISSIONING_ALL): only call new method.
 */
public class DecommissioningAllStrategy extends AbstractMigrationStrategy {

    /**
     * Returns the migration status handled by this strategy.
     *
     * @return target migration status
     */
    @Override
    public MigrationStatus getStatus() {
        return MigrationStatus.DECOMMISSIONING_ALL;
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
        var newResult = invokeSafely(context.getNewMethod(), context.getArgs());
        if (newResult.isSuccess()) {
            return newResult.value();
        }
        return executeFallback(context, newResult.error());
    }
}
