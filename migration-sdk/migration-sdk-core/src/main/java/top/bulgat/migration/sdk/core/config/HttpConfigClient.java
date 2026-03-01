package top.bulgat.migration.sdk.core.config;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
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
import top.bulgat.migration.sdk.core.model.GrayscaleConfig;
import top.bulgat.migration.sdk.core.model.MigrationConfig;
import top.bulgat.migration.sdk.core.spi.ConfigClient;

/**
 * HTTP-based config client for reading migration settings from
 * migration-admin-api.
 */
public class HttpConfigClient implements ConfigClient {

    private static final int SUCCESS_CODE_0 = 0;
    private static final int SUCCESS_CODE_200 = 200;

    private final String baseUrl;
    private final CloseableHttpClient httpClient;
    private final String internalToken;

    /**
     * Creates client from SDK properties.
     *
     * @param properties sdk runtime properties
     */
    public HttpConfigClient(MigrationSdkProperties properties) {
        this(
                trimTrailingSlash(properties.getConfigCenterUrl()),
                createHttpClient(properties.getDefaultTimeout()),
                properties.getInternalToken());
    }

    /**
     * Constructor for tests and custom injection.
     *
     * @param baseUrl       admin-api base url
     * @param httpClient    http client
     * @param internalToken token for M2M authentication
     */
    HttpConfigClient(String baseUrl, CloseableHttpClient httpClient, String internalToken) {
        this.baseUrl = trimTrailingSlash(baseUrl);
        this.httpClient = httpClient;
        this.internalToken = internalToken;
    }

    /**
     * Queries migration task config.
     *
     * @param migrationKey migration key
     * @return migration config
     */
    @Override
    public MigrationConfig getMigrationConfig(String migrationKey) {
        JSONObject payload = new JSONObject();
        payload.put("migration_key", migrationKey);
        String body = executePost("/api/v1/migration_task/query", payload.toJSONString());

        JSONObject root = JSON.parseObject(body);
        ensureSuccess(root, "query migration task");
        JSONObject data = root.getJSONObject("data");
        if (data == null) {
            throw new IllegalStateException("missing migration task response data");
        }

        Integer status = data.getInteger("status");
        if (status == null) {
            status = 1;
        }
        return MigrationConfig.builder()
                .migrationKey(data.getString("migration_key"))
                .status(status)
                .description(data.getString("description"))
                .timeout(data.getInteger("timeout"))
                .build();
    }

    /**
     * Queries grayscale rules.
     *
     * @param migrationKey migration key
     * @return grayscale rule list
     */
    @Override
    public List<GrayscaleConfig> getGrayscaleRules(String migrationKey) {
        String encodedKey = URLEncoder.encode(migrationKey, StandardCharsets.UTF_8);
        String path = "/api/v1/grayscale_rule/list?migration_key=" + encodedKey + "&page=1&pageSize=200";
        String body = executeGet(path);

        JSONObject root = JSON.parseObject(body);
        ensureSuccess(root, "query grayscale rules");
        JSONObject data = root.getJSONObject("data");
        if (data == null) {
            return List.of();
        }
        JSONArray list = data.getJSONArray("list");
        if (list == null || list.isEmpty()) {
            return List.of();
        }

        List<GrayscaleConfig> rules = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            JSONObject item = list.getJSONObject(i);
            rules.add(GrayscaleConfig.builder()
                    .ruleId(item.getString("rule_id"))
                    .migrationKey(item.getString("migration_key"))
                    .ruleType(item.getString("rule_type"))
                    .ruleValue(item.getString("rule_value"))
                    .enable(item.getBoolean("enable"))
                    .build());
        }
        return rules;
    }

    /**
     * Closes underlying HTTP client.
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
     * Executes HTTP GET request.
     *
     * @param path request path
     * @return response body
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
     * Executes HTTP POST request.
     *
     * @param path     request path
     * @param jsonBody json request body
     * @return response body
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
     * Reads response body and validates status code.
     *
     * @param response http response
     * @param action   action label used in exception message
     * @return response text
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
     * Validates admin-api standard response code.
     *
     * @param root   parsed response json
     * @param action action label used in exception message
     */
    private void ensureSuccess(JSONObject root, String action) {
        if (root == null) {
            throw new IllegalStateException(action + " failed, empty response");
        }
        Integer code = root.getInteger("code");
        if (code == null) {
            return;
        }
        if (code == SUCCESS_CODE_0 || code == SUCCESS_CODE_200) {
            return;
        }
        throw new IllegalStateException(action + " failed, code=" + code + ", message=" + root.getString("message"));
    }

    /**
     * Creates an HTTP client with unified timeout settings.
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
        return HttpClients.custom()
                .setDefaultRequestConfig(requestConfig)
                .build();
    }

    /**
     * Normalizes url value and provides default when blank.
     *
     * @param value raw url
     * @return normalized url
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
