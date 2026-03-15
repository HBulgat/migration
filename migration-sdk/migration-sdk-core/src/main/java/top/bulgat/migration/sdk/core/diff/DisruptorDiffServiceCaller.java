package top.bulgat.migration.sdk.core.diff;

import com.alibaba.fastjson2.JSONObject;
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

/**
 * 基于 Disruptor 的异步 Diff 调用器。
 */
public class DisruptorDiffServiceCaller implements DiffServiceCaller {

    private static final Logger log = LoggerFactory.getLogger(DisruptorDiffServiceCaller.class);
    private static final int RING_BUFFER_SIZE = 1024 * 1024;

    private final String diffServiceUrl;
    private final CloseableHttpClient httpClient;
    private final Disruptor<DiffEvent> disruptor;
    private final RingBuffer<DiffEvent> ringBuffer;

    /**
     * 根据 SDK 配置创建 Diff 调用器。
     *
     * @param properties SDK 运行时配置
     */
    public DisruptorDiffServiceCaller(MigrationSdkProperties properties) {
        this(trimTrailingSlash(properties.getDiffServiceAddress()), createHttpClient(properties.getConfigCenterCacheRefreshIntervalSeconds()));
    }

    /**
     * 供测试或自定义注入使用的构造函数。
     *
     * @param diffServiceUrl Diff 服务地址
     * @param httpClient     HTTP 客户端
     */
    DisruptorDiffServiceCaller(String diffServiceUrl, CloseableHttpClient httpClient) {
        this.diffServiceUrl = trimTrailingSlash(diffServiceUrl);
        this.httpClient = httpClient;

        this.disruptor = new Disruptor<>(
                DiffEvent::new,
                RING_BUFFER_SIZE,
                Executors.defaultThreadFactory(),
                ProducerType.MULTI,
                new BlockingWaitStrategy());

        this.disruptor.handleEventsWith((event, sequence, endOfBatch) -> send(event.request));
        this.disruptor.start();
        this.ringBuffer = disruptor.getRingBuffer();
    }

    /**
     * 将 Diff 请求发布到 Disruptor 队列。
     *
     * @param request Diff 请求
     */
    @Override
    public void executeDiffAsync(DiffRequest request) {
        if (request == null) {
            return;
        }
        long sequence = ringBuffer.next();
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
    private void send(DiffRequest request) {
        if (request == null) {
            return;
        }

        JSONObject payload = new JSONObject();
        payload.put("migration_key", request.getMigrationKey());
        payload.put("trace_id", request.getTraceId());
        payload.put("old_json", request.getOldJson());
        payload.put("new_json", request.getNewJson());
        payload.put("old_cost_time_ms", request.getOldCostTimeMs());
        payload.put("new_cost_time_ms", request.getNewCostTimeMs());
        payload.put("grayscale_param", request.getGrayscaleParam());
        payload.put("old_success", request.getOldSuccess());
        payload.put("new_success", request.getNewSuccess());
        payload.put("old_error_message", request.getOldErrorMessage());
        payload.put("new_error_message", request.getNewErrorMessage());
        payload.put("old_request_params", request.getOldRequestParams());
        payload.put("new_request_params", request.getNewRequestParams());
        payload.put("migration_status", request.getMigrationTaskStatus());
        payload.put("grayscale_rules", request.getGrayscaleRules());
        payload.put("grayscale_hit", request.getGrayscaleHit());
        payload.put("fallback_triggered", request.getFallbackTriggered());

        HttpPost post = new HttpPost(diffServiceUrl + "/api/v1/diff");
        post.setEntity(new StringEntity(payload.toJSONString(), StandardCharsets.UTF_8));
        post.setHeader("Content-Type", "application/json");

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
     * 创建带超时配置的 HTTP 客户端。
     *
     * @param timeout 超时时间（毫秒）
     * @return HTTP 客户端
     */
    private static CloseableHttpClient createHttpClient(int timeout) {
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(timeout)
                .setConnectionRequestTimeout(timeout)
                .setSocketTimeout(timeout)
                .build();
        return HttpClients.custom().setDefaultRequestConfig(requestConfig).build();
    }

    /**
     * 规范化 URL，并在为空时提供默认值。
     *
     * @param value 原始 URL value
     * @return 规范化后的 URL
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
