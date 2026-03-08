package top.bulgat.migration.sdk.core.strategy;

import com.alibaba.fastjson2.JSON;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.bulgat.common.base.thread.ThreadContext;
import top.bulgat.migration.sdk.core.model.DiffRequest;
import top.bulgat.migration.sdk.core.support.InvocationResult;
import top.bulgat.migration.sdk.core.support.OldInvocationFailedException;

/**
 * 基础迁移策略，提供共享的方法安全调用、并发调用以及Diff异步上报等辅助功能。
 */
public abstract class AbstractMigrationStrategy implements MigrationStrategy {

    private static final Logger log = LoggerFactory.getLogger(AbstractMigrationStrategy.class);

    /**
     * 安全地调用指定方法，并记录方法执行的耗时。
     *
     * @param method 待调用的具体方法引用
     * @param args   方法入参
     * @param <T>    返回值类型
     * @return 包含调用结果及耗时的InvocationResult对象
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
     * 在调用线程（主线程）执行旧接口，在异步线程执行新接口。
     *
     * @param context 执行上下文
     * @param <T>     返回值类型
     * @return 新旧方法的并发调用结果
     */
    protected <T> ConcurrentInvocationResult<T> invokeOldMainNewAsync(MigrationExecutionContext<T> context) {
        CompletableFuture<InvocationResult<T>> newFuture = CompletableFuture.supplyAsync(
                () -> invokeSafely(context.getNewMethod(), context.getArgs()),
                context.getExecutorService());
        InvocationResult<T> oldResult = invokeSafely(context.getOldMethod(), context.getArgs());
        InvocationResult<T> newResult = newFuture.join();
        return new ConcurrentInvocationResult<>(oldResult, newResult);
    }

    /**
     * 在调用线程（主线程）执行新接口，在异步线程执行旧接口。
     *
     * @param context 执行上下文
     * @param <T>     返回值类型
     * @return 新旧方法的并发调用结果
     */
    protected <T> ConcurrentInvocationResult<T> invokeNewMainOldAsync(MigrationExecutionContext<T> context) {
        CompletableFuture<InvocationResult<T>> oldFuture = CompletableFuture.supplyAsync(
                () -> invokeSafely(context.getOldMethod(), context.getArgs()),
                context.getExecutorService());
        InvocationResult<T> newResult = invokeSafely(context.getNewMethod(), context.getArgs());
        InvocationResult<T> oldResult = oldFuture.join();
        return new ConcurrentInvocationResult<>(oldResult, newResult);
    }

    /**
     * 执行降级方法。
     *
     * @param context 执行上下文
     * @param ex      触发降级的异常信息
     * @param <T>     返回值类型
     * @return 降级处理结果
     */
    protected <T> T executeFallback(MigrationExecutionContext<T> context, Exception ex) {
        return context.getFallbackMethod().apply(context.getArgs(), ex);
    }

    /**
     * 当旧接口方法已经执行且失败时，处理降级逻辑。
     * <p>
     * 对于默认的降级处理（即再次调用旧接方法），为避免重复抛出相同的异常，
     * 会通过抛出一个指定类型的标志异常（OldInvocationFailedException）来传递该错误。
     *
     * @param context  执行上下文
     * @param oldError 旧方法调用产生的异常
     * @param <T>      返回值类型
     * @return 降级执行结果
     */
    protected <T> T executeFallbackAfterOldFailed(MigrationExecutionContext<T> context, Exception oldError) {
        if (context.isDefaultOldFallback()) {
            throw new OldInvocationFailedException(oldError);
        }
        return executeFallback(context, oldError);
    }

    /**
     * 异步发送Diff请求；发生异常时仅打印日志。
     * 即使新旧接口调用失败，也会发送请求以记录调用详情。
     *
     * @param context           执行上下文
     * @param oldResult         旧接口调用结果
     * @param newResult         新接口调用结果
     * @param grayscaleParam    用于发送的灰度参数
     * @param grayscaleHit      是否命中灰度规则
     * @param fallbackTriggered 是否触发降级
     * @param <T>               返回值类型
     */
    protected <T> void sendDiffAsync(
            MigrationExecutionContext<T> context,
            InvocationResult<T> oldResult,
            InvocationResult<T> newResult,
            Map<String, Object> grayscaleParam,
            boolean grayscaleHit,
            boolean fallbackTriggered) {
        try {
            context.getDiffServiceCaller().executeDiffAsync(DiffRequest.builder()
                    .migrationKey(context.getMigrationKey())
                    .traceId(ThreadContext.getTraceId())
                    .oldJson(oldResult.isSuccess() ? JSON.toJSONString(oldResult.value()) : null)
                    .newJson(newResult.isSuccess() ? JSON.toJSONString(newResult.value()) : null)
                    .oldCostTimeMs((int) oldResult.costTimeMs())
                    .newCostTimeMs((int) newResult.costTimeMs())
                    .grayscaleParam(JSON.toJSONString(grayscaleParam))
                    .oldSuccess(oldResult.isSuccess())
                    .newSuccess(newResult.isSuccess())
                    .oldErrorMessage(oldResult.error() != null ? oldResult.error().getMessage() : null)
                    .newErrorMessage(newResult.error() != null ? newResult.error().getMessage() : null)
                    .oldRequestParams(JSON.toJSONString(context.getArgs()))
                    .newRequestParams(JSON.toJSONString(context.getArgs()))
                    .MigrationTaskStatus(context.getMigrationTaskStatus())
                    .grayscaleRules(JSON.toJSONString(context.getGrayscaleRules()))
                    .grayscaleHit(grayscaleHit)
                    .fallbackTriggered(fallbackTriggered)
                    .build());
        } catch (Exception ex) {
            log.warn("send diff async failed, migrationKey={}", context.getMigrationKey(), ex);
        }
    }

    /**
     * 评估并匹配灰度规则。
     *
     * @param context        执行上下文
     * @param grayscaleParam 提供的灰度参数
     * @return 如果命中规则，则返回true；否则返回false
     */
    protected boolean matchGrayscale(MigrationExecutionContext<?> context, Map<String, Object> grayscaleParam) {
        return context.getGrayscaleMatcher().match(context.getGrayscaleRules(), grayscaleParam);
    }

    /**
     * 用于保存并发调用新旧接口产生的结果持有的记录类。
     *
     * @param oldResult 旧接口结果
     * @param newResult 新接口结果
     * @param <T>       返回值类型
     */
    protected record ConcurrentInvocationResult<T>(InvocationResult<T> oldResult, InvocationResult<T> newResult) {
    }
}
