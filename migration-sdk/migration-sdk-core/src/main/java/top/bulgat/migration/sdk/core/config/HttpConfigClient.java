package top.bulgat.migration.sdk.core.config;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.TypeReference;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import top.bulgat.common.base.model.Result;
import top.bulgat.migration.sdk.core.model.GrayscaleConfig;
import top.bulgat.migration.sdk.core.model.MigrationConfig;
import top.bulgat.migration.sdk.core.spi.ConfigClient;

/**
 * 基于 HTTP 的配置客户端，用于从 migration-admin-api 读取迁移配置。
 */
public class HttpConfigClient implements ConfigClient {

    private static final int SUCCESS_CODE_0 = 0;
    private static final int SUCCESS_CODE_200 = 200;

    private final String baseUrl;
    private final CloseableHttpClient httpClient;
    private final String internalToken;

    /**
     * 根据 SDK 配置创建客户端。
     *
     * @param properties SDK 运行时配置
     */
    public HttpConfigClient(MigrationSdkProperties properties) {
        this(
                trimTrailingSlash(properties.getConfigCenterAddress()),
                createHttpClient(properties.getConfigCenterTimeout()),
                properties.getDiffServiceInternalToken());
    }

    /**
     * 供测试或自定义注入使用的构造函数。
     *
     * @param baseUrl       Admin API 基础地址
     * @param httpClient    HTTP 客户端
     * @param internalToken 机器间认证令牌
     */
    HttpConfigClient(String baseUrl, CloseableHttpClient httpClient, String internalToken) {
        this.baseUrl = trimTrailingSlash(baseUrl);
        this.httpClient = httpClient;
        this.internalToken = internalToken;
    }

    /**
     * 查询迁移任务配置。
     *
     * @param migrationKey 迁移标识
     * @return 迁移配置
     */
    @Override
    public MigrationConfig getMigrationConfig(String migrationKey) {
        JSONObject payload = new JSONObject();
        payload.put("migration_key", migrationKey);
        String body = executePost("/api/internal/sdk/migration_task/query", payload.toJSONString());

        Result<MigrationConfig> result = JSON.parseObject(body, new TypeReference<Result<MigrationConfig>>() {
        });
        ensureSuccess(result, "query migration task");
        if (result.getData() == null) {
            throw new IllegalStateException("missing migration task response data");
        }

        MigrationConfig config = result.getData();
        if (config.getStatus() == null) {
            config.setStatus(1);
        }
        return config;
    }

    /**
     * 查询灰度规则。
     *
     * @param migrationKey 迁移标识
     * @return 灰度规则列表
     */
    @Override
    public List<GrayscaleConfig> getGrayscaleRules(String migrationKey) {
        String encodedKey = URLEncoder.encode(migrationKey, StandardCharsets.UTF_8);
        String path = "/api/internal/sdk/grayscale_rule/list?migration_key=" + encodedKey;
        String body = executeGet(path);

        Result<List<GrayscaleConfig>> result = JSON.parseObject(body,
                new TypeReference<>() {
                });
        ensureSuccess(result, "query grayscale rules");

        if (result.getData() == null || result.getData().isEmpty()) {
            return List.of();
        }
        return result.getData();
    }

    /**
     * 关闭底层 HTTP 客户端。
     */
    @Override
    public void close() {
        try {
            httpClient.close();
        } catch (IOException ex) {
            throw new IllegalStateException("close http config client failed", ex);
        }
    }

    /**
     * 执行 HTTP GET 请求。
     *
     * @param path 请求路径
     * @return 响应体
     */
    private String executeGet(String path) {
        HttpGet request = new HttpGet(baseUrl + path);
        if (internalToken != null && !internalToken.isEmpty()) {
            request.setHeader("X-Internal-Token", internalToken);
        }
        try (CloseableHttpResponse response = httpClient.execute(request)) {
            return readBody(response, "http get failed");
        } catch (IOException ex) {
            throw new IllegalStateException("http get failed", ex);
        }
    }

    /**
     * 执行 HTTP POST 请求。
     *
     * @param path     请求路径
     * @param jsonBody JSON 请求体
     * @return 响应体
     */
    private String executePost(String path, String jsonBody) {
        HttpPost request = new HttpPost(baseUrl + path);
        if (internalToken != null && !internalToken.isEmpty()) {
            request.setHeader("X-Internal-Token", internalToken);
        }
        request.setEntity(new StringEntity(jsonBody, ContentType.APPLICATION_JSON));
        try (CloseableHttpResponse response = httpClient.execute(request)) {
            return readBody(response, "http post failed");
        } catch (IOException ex) {
            throw new IllegalStateException("http post failed", ex);
        }
    }

    /**
     * 读取响应体并校验状态码。
     *
     * @param response HTTP 响应
     * @param action   异常信息中的动作描述
     * @return 响应文本
     */
    private String readBody(CloseableHttpResponse response, String action) {
        int statusCode = response.getStatusLine().getStatusCode();
        HttpEntity entity = response.getEntity();
        String body;
        try {
            body = entity == null ? "" : EntityUtils.toString(entity, StandardCharsets.UTF_8);
        } catch (Exception ex) {
            throw new IllegalStateException(action + ", parse body failed", ex);
        }
        if (statusCode >= 400) {
            throw new IllegalStateException(action + ", status=" + statusCode + ", body=" + body);
        }
        return body;
    }

    /**
     * 校验 admin-api 标准响应码。
     *
     * @param result 解析后的响应
     * @param action 异常信息中的动作描述
     */
    private void ensureSuccess(Result<?> result, String action) {
        if (result == null) {
            throw new IllegalStateException(action + " failed, empty response");
        }
        int code = result.getCode();
        if (code == SUCCESS_CODE_0 || code == SUCCESS_CODE_200) {
            return;
        }
        throw new IllegalStateException(action + " failed, code=" + code + ", message=" + result.getMessage());
    }

    /**
     * 创建统一超时配置的 HTTP 客户端。
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
        return HttpClients.custom()
                .setDefaultRequestConfig(requestConfig)
                .build();
    }

    /**
     * 规范化 URL，并在为空时提供默认值。
     *
     * @param value 原始 URL
     * @return 规范化后的 URL
     */
    private static String trimTrailingSlash(String value) {
        if (value == null || value.isBlank()) {
            return "http://localhost:8080";
        }
        if (value.endsWith("/")) {
            return value.substring(0, value.length() - 1);
        }
        return value;
    }
}
