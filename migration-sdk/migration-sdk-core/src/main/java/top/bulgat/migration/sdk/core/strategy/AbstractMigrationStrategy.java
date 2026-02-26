package top.bulgat.migration.sdk.core.strategy;

import com.alibaba.fastjson2.JSON;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.bulgat.common.thread.ThreadContext;
import top.bulgat.migration.sdk.core.model.DiffRequest;
import top.bulgat.migration.sdk.core.support.InvocationResult;
import top.bulgat.migration.sdk.core.support.OldInvocationFailedException;

/**
 * Base migration strategy with shared invocation and diff-report helpers.
 */
public abstract class AbstractMigrationStrategy implements MigrationStrategy {

    private static final Logger log = LoggerFactory.getLogger(AbstractMigrationStrategy.class);

    /**
     * Invokes method safely and records elapsed time.
     */
    protected <T> InvocationResult<T> invokeSafely(Function<Object[], T> method, Object[] args) {
        long start = System.currentTimeMillis();
        try {
            return InvocationResult.success(method.apply(args), System.currentTimeMillis() - start);
        } catch (Exception ex) {
            return InvocationResult.failure(ex, System.currentTimeMillis() - start);
        }
    }

    /**
     * Executes old method on caller thread and new method asynchronously.
     */
    protected <T> ConcurrentInvocationResult<T> invokeOldMainNewAsync(MigrationExecutionContext<T> context) {
        CompletableFuture<InvocationResult<T>> newFuture =
                CompletableFuture.supplyAsync(
                        () -> invokeSafely(context.getNewMethod(), context.getArgs()),
                        context.getExecutorService());
        InvocationResult<T> oldResult = invokeSafely(context.getOldMethod(), context.getArgs());
        InvocationResult<T> newResult = newFuture.join();
        return new ConcurrentInvocationResult<>(oldResult, newResult);
    }

    /**
     * Executes new method on caller thread and old method asynchronously.
     */
    protected <T> ConcurrentInvocationResult<T> invokeNewMainOldAsync(MigrationExecutionContext<T> context) {
        CompletableFuture<InvocationResult<T>> oldFuture =
                CompletableFuture.supplyAsync(
                        () -> invokeSafely(context.getOldMethod(), context.getArgs()),
                        context.getExecutorService());
        InvocationResult<T> newResult = invokeSafely(context.getNewMethod(), context.getArgs());
        InvocationResult<T> oldResult = oldFuture.join();
        return new ConcurrentInvocationResult<>(oldResult, newResult);
    }

    /**
     * Executes fallback method.
     */
    protected <T> T executeFallback(MigrationExecutionContext<T> context, Exception ex) {
        return context.getFallbackMethod().apply(context.getArgs(), ex);
    }

    /**
     * Handles fallback when old method has already executed and failed.
     * <p>
     * For default fallback (calling old method again), avoid duplicate invocation
     * by propagating old failure through a marker exception.
     */
    protected <T> T executeFallbackAfterOldFailed(MigrationExecutionContext<T> context, Exception oldError) {
        if (context.isDefaultOldFallback()) {
            throw new OldInvocationFailedException(oldError);
        }
        return executeFallback(context, oldError);
    }

    /**
     * Sends diff request asynchronously; failures are logged only.
     */
    protected <T> void sendDiffAsync(
            MigrationExecutionContext<T> context,
            InvocationResult<T> oldResult,
            InvocationResult<T> newResult,
            Map<String, Object> grayscaleParam) {
        if (!oldResult.isSuccess() || !newResult.isSuccess()) {
            return;
        }
        try {
            context.getDiffServiceCaller().executeDiffAsync(DiffRequest.builder()
                    .migrationKey(context.getMigrationKey())
                    .traceId(ThreadContext.getTraceId())
                    .oldJson(JSON.toJSONString(oldResult.value()))
                    .newJson(JSON.toJSONString(newResult.value()))
                    .oldCostTimeMs((int) oldResult.costTimeMs())
                    .newCostTimeMs((int) newResult.costTimeMs())
                    .grayscaleParam(JSON.toJSONString(grayscaleParam))
                    .build());
        } catch (Exception ex) {
            log.warn("send diff async failed, migrationKey={}", context.getMigrationKey(), ex);
        }
    }

    /**
     * Evaluates grayscale rules.
     */
    protected boolean matchGrayscale(MigrationExecutionContext<?> context, Map<String, Object> grayscaleParam) {
        return context.getGrayscaleMatcher().match(context.getGrayscaleRules(), grayscaleParam);
    }

    /**
     * Concurrent invocation result holder.
     */
    protected record ConcurrentInvocationResult<T>(InvocationResult<T> oldResult, InvocationResult<T> newResult) {
    }
}
