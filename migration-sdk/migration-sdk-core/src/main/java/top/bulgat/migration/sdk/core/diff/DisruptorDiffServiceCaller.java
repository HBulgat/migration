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
 * Disruptor-based async diff caller.
 */
public class DisruptorDiffServiceCaller implements DiffServiceCaller {

    private static final Logger log = LoggerFactory.getLogger(DisruptorDiffServiceCaller.class);
    private static final int RING_BUFFER_SIZE = 1024 * 1024;

    private final String diffServiceUrl;
    private final CloseableHttpClient httpClient;
    private final Disruptor<DiffEvent> disruptor;
    private final RingBuffer<DiffEvent> ringBuffer;

    /**
     * Creates diff caller from SDK properties.
     *
     * @param properties sdk runtime properties
     */
    public DisruptorDiffServiceCaller(MigrationSdkProperties properties) {
        this(trimTrailingSlash(properties.getDiffServiceUrl()), createHttpClient(properties.getDefaultTimeout()));
    }

    /**
     * Constructor for tests and custom injection.
     *
     * @param diffServiceUrl diff service url
     * @param httpClient http client
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
     * Publishes diff request to disruptor queue.
     *
     * @param request diff request
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
     * Closes disruptor and HTTP client resources.
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
     * Sends one diff request to remote diff service.
     *
     * @param request diff request
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

        HttpPost post = new HttpPost(diffServiceUrl + "/api/v1/diff");
        post.setEntity(new StringEntity(payload.toJSONString(), StandardCharsets.UTF_8));
        post.setHeader("Content-Type", "application/json");

        try (CloseableHttpResponse response = httpClient.execute(post)) {
            int status = response.getStatusLine().getStatusCode();
            if (status >= 400) {
                HttpEntity entity = response.getEntity();
                String body = entity == null ? "" : EntityUtils.toString(entity, StandardCharsets.UTF_8);
                log.warn("diff request failed, status={}, migrationKey={}, body={}",
                        status,
                        request.getMigrationKey(),
                        body);
            }
        } catch (Exception ex) {
            log.warn("diff request failed, migrationKey={}", request.getMigrationKey(), ex);
        }
    }

    /**
     * Creates HTTP client with timeout settings.
     *
     * @param timeout timeout in milliseconds
     * @return http client
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
     * Normalizes url value and provides default when blank.
     *
     * @param value raw url value
     * @return normalized url
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
     * Disruptor event container.
     */
    private static final class DiffEvent {
        private DiffRequest request;
    }
}
