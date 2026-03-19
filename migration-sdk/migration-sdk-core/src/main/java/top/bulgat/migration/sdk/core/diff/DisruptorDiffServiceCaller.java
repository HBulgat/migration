package top.bulgat.migration.sdk.core.diff;

import com.alibaba.fastjson2.JSON;
import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.bulgat.migration.sdk.core.config.MigrationSdkProperties;
import top.bulgat.migration.sdk.core.model.DiffRequest;
import top.bulgat.migration.sdk.core.spi.DiffServiceCaller;

import com.lmax.disruptor.WorkHandler;
import com.lmax.disruptor.InsufficientCapacityException;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;

/**
 * 基于 Disruptor 的异步 Diff 调用器。
 */
public class DisruptorDiffServiceCaller implements DiffServiceCaller {

    private static final Logger log = LoggerFactory.getLogger(DisruptorDiffServiceCaller.class);
    private static final int RING_BUFFER_SIZE = 1024 * 1024;

    private final String diffServiceUrl;
    private final String internalToken;
    private final CloseableHttpClient httpClient;
    private final Disruptor<DiffEvent> disruptor;
    private final RingBuffer<DiffEvent> ringBuffer;
    private final AtomicLong dropCount = new AtomicLong(0);

    /**
     * 根据 SDK 配置创建 Diff 调用器。
     *
     * @param properties SDK 运行时配置
     */
    public DisruptorDiffServiceCaller(MigrationSdkProperties properties) {
        this(trimTrailingSlash(properties.getDiffServiceAddress()), 
             createHttpClient(properties.getDiffServiceTimeout(), properties.getDiffServiceMaxConnections()),
             properties.getDiffServiceInternalToken(),
             properties.getDiffServiceWorkerCount());
    }

    /**
     * 供测试或自定义注入使用的构造函数。
     */
    DisruptorDiffServiceCaller(String diffServiceUrl, CloseableHttpClient httpClient, String internalToken, Integer workerCount) {
        this.diffServiceUrl = trimTrailingSlash(diffServiceUrl);
        this.httpClient = httpClient;
        this.internalToken = internalToken;
        this.disruptor = new Disruptor<>(
                DiffEvent::new,
                RING_BUFFER_SIZE,
                Executors.defaultThreadFactory(),
                ProducerType.MULTI,
                new BlockingWaitStrategy());

        // 使用 WorkerPool 实现多线程并发消费，提升吞吐量
        int threads = workerCount != null && workerCount > 0 ? workerCount : 4;
        DiffWorkHandler[] handlers = new DiffWorkHandler[threads];
        for (int i = 0; i < threads; i++) {
            handlers[i] = new DiffWorkHandler();
        }
        this.disruptor.handleEventsWithWorkerPool(handlers);
        
        this.disruptor.start();
        this.ringBuffer = disruptor.getRingBuffer();
        log.info("[Migration-SDK] DisruptorDiffServiceCaller started with {} workers.", threads);
    }

    /**
     * 实现 WorkHandler 接口，支持 WorkerPool 多线程消费。
     */
    private class DiffWorkHandler implements WorkHandler<DiffEvent> {
        @Override
        public void onEvent(DiffEvent event) {
            if (event != null && event.request != null) {
                send(event.request);
                // 处理完后清空引用，协助 GC
                event.request = null;
            }
        }
    }

    /**
     * 非阻塞式地将 Diff 请求发布到 Disruptor 队列。
     * 如果队列已满，直接丢弃以保证业务链路不被阻塞。
     *
     * @param request Diff 请求
     */
    @Override
    public void executeDiffAsync(DiffRequest request) {
        if (request == null) {
            return;
        }
        
        long sequence;
        try {
            // 尝试获取可用位次，如果队列满则直接抛出异常
            sequence = ringBuffer.tryNext();
        } catch (InsufficientCapacityException e) {
            long totalDropped = dropCount.incrementAndGet();
            if (totalDropped % 100 == 1) { // 采样日志，避免日志爆炸
                log.warn("[Migration-SDK] Diff queue is full, dropping request. Total dropped: {}", totalDropped);
            }
            return;
        }

        try {
            DiffEvent event = ringBuffer.get(sequence);
            event.request = request;
        } finally {
            ringBuffer.publish(sequence);
        }
    }

    /**
     * 关闭 Disruptor 和 HTTP 客户端资源。
     */
    @Override
    public void close() {
        try {
            disruptor.shutdown();
        } catch (Exception ex) {
            log.warn("shutdown disruptor failed", ex);
        }
        try {
            httpClient.close();
        } catch (IOException ex) {
            throw new IllegalStateException("close diff caller failed", ex);
        }
    }

    /**
     * 向远端 Diff 服务发送一次请求。
     *
     * @param request Diff 请求
     */
    /**
     * 向远端 Diff 服务发送一次请求。
     *
     * @param request Diff 请求
     */
    private void send(DiffRequest request) {
        if (request == null) {
            return;
        }

        String jsonPayload = JSON.toJSONString(request);

        HttpPost post = new HttpPost(diffServiceUrl + "/api/v1/diff");
        post.setEntity(new StringEntity(jsonPayload, StandardCharsets.UTF_8));
        post.setHeader("Content-Type", "application/json");
        post.setHeader("X-Internal-Token", internalToken);
        try (CloseableHttpResponse response = httpClient.execute(post)) {
            int status = response.getStatusLine().getStatusCode();
            if (status >= 400) {
                HttpEntity entity = response.getEntity();
                String body = entity == null ? "" : EntityUtils.toString(entity, StandardCharsets.UTF_8);
                log.warn("Diff 请求 failed, status={}, migrationKey={}, body={}",
                        status,
                        request.getMigrationKey(),
                        body);
            }
        } catch (Exception ex) {
            log.warn("Diff 请求 failed, migrationKey={}", request.getMigrationKey(), ex);
        }
    }

    /**
     * 创建带超时和连接池配置的 HTTP 客户端。
     */
    private static CloseableHttpClient createHttpClient(int timeout, Integer maxConnections) {
        int max = maxConnections != null && maxConnections > 0 ? maxConnections : 100;
        
        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
        cm.setMaxTotal(max);
        cm.setDefaultMaxPerRoute(max);

        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(timeout)
                .setConnectionRequestTimeout(timeout)
                .setSocketTimeout(timeout)
                .build();
        
        return HttpClients.custom()
                .setConnectionManager(cm)
                .setDefaultRequestConfig(requestConfig)
                .build();
    }

    /**
     * 规范化 URL，并在为空时提供默认值。
     */
    private static String trimTrailingSlash(String value) {
        if (value == null || value.isBlank()) {
            return "http://localhost:8081";
        }
        if (value.endsWith("/")) {
            return value.substring(0, value.length() - 1);
        }
        return value;
    }

    /**
     * Disruptor 事件容器。
     */
    private static final class DiffEvent {
        private DiffRequest request;
    }
}
