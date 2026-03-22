package top.bulgat.migration.sdk.core.strategy;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import top.bulgat.common.base.thread.ThreadContext;
import top.bulgat.migration.sdk.core.model.MigrationTaskStatus;
import top.bulgat.migration.sdk.core.spi.DiffServiceCaller;
import top.bulgat.migration.sdk.core.spi.GrayMatcher;
import top.bulgat.migration.sdk.core.support.InvocationResult;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class StrategyConcurrencyTest {

    private final ExecutorService executorService = Executors.newFixedThreadPool(2);

    @Test
    public void testInvokeOldMainNewAsync_NoBlocking() throws InterruptedException {
        // 1. 模拟环境
        ValidationAllStrategy strategy = new ValidationAllStrategy();
        CountDownLatch slowNewLatch = new CountDownLatch(1);
        AtomicBoolean diffReported = new AtomicBoolean(false);
        String expectedTraceId = "test-trace-id-123";

        MigrationExecutionContext<String> context = MigrationExecutionContext.<String>builder()
                .migrationKey("test_key")
                .migrationTaskStatus(MigrationTaskStatus.VALIDATION_ALL.getCode())
                .args(new Object[]{})
                .executorService(executorService)
                // 旧方法：秒回
                .oldMethod(args -> "old_val")
                // 新方法：故意慢 500ms
                .newMethod(args -> {
                    try {
                        slowNewLatch.await();
                        return "new_val";
                    } catch (InterruptedException e) {
                        return "error";
                    }
                })
                .diffServiceCaller(req -> {
                    // 验证 TraceID 传递
                    if (expectedTraceId.equals(req.getTraceId())) {
                        diffReported.set(true);
                    }
                })
                .grayMatcher((rules, params) -> true)
                .build();

        // 2. 执行并计时
        ThreadContext.setTraceId(expectedTraceId);
        long start = System.currentTimeMillis();
        String result = strategy.execute(context);
        long end = System.currentTimeMillis();

        // 3. 验证
        Assertions.assertEquals("old_val", result);
        // 主线程耗时应该远小于 500ms
        Assertions.assertTrue((end - start) < 200, "Main thread should not be blocked by slow new interface, actual cost: " + (end - start));

        // 释放新接口，让后台上报完成
        slowNewLatch.countDown();
        
        // 等待后台上报（如果不加这个，Diff 数据还没上来测试就结束了）
        TimeUnit.MILLISECONDS.sleep(100);
        Assertions.assertTrue(diffReported.get(), "Diff should be reported in background with correct traceId");
        
        ThreadContext.clear();
    }

    @Test
    public void testInvokeNewMainOldAsync_NoBlocking() throws InterruptedException {
        // 模拟 GoLiveAllStrategy
        GoLiveAllStrategy strategy = new GoLiveAllStrategy();
        CountDownLatch slowOldLatch = new CountDownLatch(1);
        AtomicBoolean diffReported = new AtomicBoolean(false);
        String expectedTraceId = "test-trace-id-456";

        MigrationExecutionContext<String> context = MigrationExecutionContext.<String>builder()
                .migrationKey("test_key")
                .migrationTaskStatus(MigrationTaskStatus.GO_LIVE_ALL.getCode())
                .args(new Object[]{})
                .executorService(executorService)
                // 旧方法：故意慢 500ms
                .oldMethod(args -> {
                    try {
                        slowOldLatch.await();
                        return "old_val";
                    } catch (InterruptedException e) {
                        return "error";
                    }
                })
                // 新方法：秒回
                .newMethod(args -> "new_val")
                .diffServiceCaller(req -> {
                    if (expectedTraceId.equals(req.getTraceId())) {
                        diffReported.set(true);
                    }
                })
                .build();

        // 2. 执行并计时
        ThreadContext.setTraceId(expectedTraceId);
        long start = System.currentTimeMillis();
        String result = strategy.execute(context);
        long end = System.currentTimeMillis();

        // 3. 验证
        Assertions.assertEquals("new_val", result);
        Assertions.assertTrue((end - start) < 200, "Main thread should not be blocked by slow old interface, actual cost: " + (end - start));

        slowOldLatch.countDown();
        TimeUnit.MILLISECONDS.sleep(100);
        Assertions.assertTrue(diffReported.get(), "Diff should be reported in background with correct traceId");
        
        ThreadContext.clear();
    }
}
