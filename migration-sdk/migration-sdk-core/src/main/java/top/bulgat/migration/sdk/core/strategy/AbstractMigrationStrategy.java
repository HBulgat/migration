package top.bulgat.migration.sdk.core.strategy;

import com.alibaba.fastjson2.JSON;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.RejectedExecutionException;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.bulgat.common.base.thread.ThreadContext;
import top.bulgat.migration.sdk.core.model.DiffRequest;
import top.bulgat.migration.sdk.core.extension.DiffPostProcessor;
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
     * 在调用线程（主线程）执行旧接口，在异步线程执行新接口进行 Diff。
     * 遵循主线程零阻塞原则，执行完旧接口即刻返回。
     *
     * @param context        执行上下文
     * @param grayParam 灰度参数
     * @param <T>            返回值类型
     * @return 仅包含旧接口调用结果的 ConcurrentInvocationResult
     */
    protected <T> ConcurrentInvocationResult<T> invokeOldMainNewAsync(MigrationExecutionContext<T> context, Map<String, Object> grayParam) {
        String traceId = ThreadContext.getTraceId();
        Object[] args = context.getArgs() == null ? new Object[0] : context.getArgs();

        // 用于后台线程获取主线程结果的 Future
        CompletableFuture<InvocationResult<T>> oldResultFuture = new CompletableFuture<>();

        // 异步执行：新接口 + 等待旧接口 + 上报 Diff
        try {
            CompletableFuture.runAsync(() -> {
                try {
                    ThreadContext.setTraceId(traceId);
                    // 执行新接口
                    InvocationResult<T> newResult = invokeSafely(context.getNewMethod(), args);
                    // 阻塞后台线程等待主线程执行完（带超时保护，防止死锁）
                    InvocationResult<T> oldResult = oldResultFuture.get(5, java.util.concurrent.TimeUnit.SECONDS);
                    // 后台上报
                    sendDiffAsync(context, oldResult, newResult, grayParam, true, false);
                } catch (Exception ex) {
                    log.warn("background diff task failed, migrationKey={}", context.getMigrationKey(), ex);
                } finally {
                    ThreadContext.clear();
                }
            }, context.getExecutorService());
        } catch (RejectedExecutionException e) {
            log.warn("[Migration-SDK] Background task rejected for migrationKey={}, executing synchronously.", context.getMigrationKey());
        }

        // 主线程同步执行旧接口
        InvocationResult<T> oldResult = null;
        try {
            oldResult = invokeSafely(context.getOldMethod(), args);
        } finally {
            // 无论成功失败，都必须 complete future，让后台线程继续
            oldResultFuture.complete(oldResult);
        }

        // 立即返回结果给业务主流程，绝不等待
        return new ConcurrentInvocationResult<>(oldResult, null);
    }

    /**
     * 在调用线程（主线程）执行新接口，在异步线程执行旧接口进行 Diff。
     * 遵循主线程零阻塞原则，正常情况下返回新接口结果。
     *
     * @param context        执行上下文
     * @param grayParam 灰度参数
     * @param <T>            返回值类型
     * @return 仅包含新接口调用结果的 ConcurrentInvocationResult
     */
    protected <T> ConcurrentInvocationResult<T> invokeNewMainOldAsync(MigrationExecutionContext<T> context, Map<String, Object> grayParam) {
        String traceId = ThreadContext.getTraceId();
        Object[] args = context.getArgs() == null ? new Object[0] : context.getArgs();

        // 用于后台线程获取主线程结果的 Future
        CompletableFuture<InvocationResult<T>> newResultFuture = new CompletableFuture<>();

        // 异步执行：旧接口 + 等待新接口 + 上报 Diff
        try {
            CompletableFuture.runAsync(() -> {
                try {
                    ThreadContext.setTraceId(traceId);
                    // 执行旧接口
                    InvocationResult<T> oldResult = invokeSafely(context.getOldMethod(), args);
                    // 阻塞后台线程等待主线程执行完（带超时保护）
                    InvocationResult<T> newResult = newResultFuture.get(5, java.util.concurrent.TimeUnit.SECONDS);
                    // 后台上报
                    sendDiffAsync(context, oldResult, newResult, grayParam, true, false);
                } catch (Exception ex) {
                    log.warn("background diff task failed, migrationKey={}", context.getMigrationKey(), ex);
                } finally {
                    ThreadContext.clear();
                }
            }, context.getExecutorService());
        } catch (RejectedExecutionException e) {
            log.warn("[Migration-SDK] Background task rejected for migrationKey={}, executing synchronously.", context.getMigrationKey());
        }

        // 主线程同步执行新接口
        InvocationResult<T> newResult = null;
        try {
            newResult = invokeSafely(context.getNewMethod(), args);
        } finally {
            newResultFuture.complete(newResult);
        }

        // 立即返回新接口结果
        return new ConcurrentInvocationResult<>(null, newResult);
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
     * @param grayParam    用于发送的灰度参数
     * @param grayHit      是否命中灰度规则
     * @param fallbackTriggered 是否触发降级
     * @param <T>               返回值类型
     */
    protected <T> void sendDiffAsync(
            MigrationExecutionContext<T> context,
            InvocationResult<T> oldResult,
            InvocationResult<T> newResult,
            Map<String, Object> grayParam,
            boolean grayHit,
            boolean fallbackTriggered) {
        try {
            Object rawOld = oldResult.isSuccess() ? oldResult.value() : null;
            Object rawNew = newResult.isSuccess() ? newResult.value() : null;

            DiffPostProcessor.ProcessedResult processed = context.getPostProcessor()
                    .process(context.getMigrationKey(), rawOld, rawNew);

            context.getDiffServiceCaller().executeDiffAsync(DiffRequest.builder()
                    .migrationKey(context.getMigrationKey())
                    .traceId(ThreadContext.getTraceId())
                    .oldJson(oldResult.isSuccess() ? JSON.toJSONString(processed.processedOld()) : null)
                    .newJson(newResult.isSuccess() ? JSON.toJSONString(processed.processedNew()) : null)
                    .oldCostTimeMs((int) oldResult.costTimeMs())
                    .newCostTimeMs((int) newResult.costTimeMs())
                    .grayParam(JSON.toJSONString(grayParam))
                    .oldSuccess(oldResult.isSuccess())
                    .newSuccess(newResult.isSuccess())
                    .oldErrorMessage(oldResult.error() != null ? oldResult.error().getMessage() : null)
                    .newErrorMessage(newResult.error() != null ? newResult.error().getMessage() : null)
                    .oldRequestParams(JSON.toJSONString(context.getArgs()))
                    .newRequestParams(JSON.toJSONString(context.getArgs()))
                    .migrationStatus(context.getMigrationTaskStatus())
                    .grayRules(JSON.toJSONString(context.getGrayRules()))
                    .grayHit(grayHit)
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
     * @param grayParam 提供的灰度参数
     * @return 如果命中规则，则返回true；否则返回false
     */
    protected boolean matchGray(MigrationExecutionContext<?> context, Map<String, Object> grayParam) {
        return context.getGrayMatcher().match(context.getGrayRules(), grayParam);
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
