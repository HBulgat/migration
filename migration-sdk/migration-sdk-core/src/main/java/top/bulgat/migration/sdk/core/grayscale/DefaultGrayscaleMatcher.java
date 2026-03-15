package top.bulgat.migration.sdk.core.grayscale;

import com.alibaba.fastjson2.JSONArray;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.burt.jmespath.Expression;
import io.burt.jmespath.JmesPath;
import io.burt.jmespath.jackson.JacksonRuntime;
import top.bulgat.migration.sdk.core.model.GrayscaleConfig;
import top.bulgat.migration.sdk.core.model.GrayscaleRuleType;
import top.bulgat.migration.sdk.core.spi.GrayscaleMatcher;

/**
 * 默认灰度匹配器，支持百分比、名单和表达式规则。
 */
public class DefaultGrayscaleMatcher implements GrayscaleMatcher {

    private static final Logger log = LoggerFactory.getLogger(DefaultGrayscaleMatcher.class);
    private static final JmesPath<JsonNode> PARSER = new JacksonRuntime();
    private static final Map<String, Expression<JsonNode>> EXPRESSION_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, JSONArray> JSON_ARRAY_CACHE = new ConcurrentHashMap<>();
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * 按顺序评估灰度规则，并在首次命中时返回结果。
     *
     * @param rules  灰度规则
     * @param params 灰度参数
     * @return 命中灰度时返回 true
     */
    @Override
    public boolean match(List<GrayscaleConfig> rules, Map<String, Object> params) {
        if (rules == null || rules.isEmpty()) {
            return false;
        }

        List<GrayscaleConfig> sortedRules = new java.util.ArrayList<>(rules);
        sortedRules.sort(java.util.Comparator.comparing(GrayscaleConfig::getWeight, java.util.Comparator.nullsLast(java.util.Comparator.reverseOrder())));

        Map<String, Object> safeParams = params == null ? Collections.emptyMap() : params;

        for (GrayscaleConfig rule : sortedRules) {
            if (rule == null || !Boolean.TRUE.equals(rule.getEnable())) {
                continue;
            }
            GrayscaleRuleType type;
            try {
                type = GrayscaleRuleType.fromValue(rule.getRuleType());
            } catch (Exception ex) {
                log.warn("skip invalid grayscale rule type, type={}", rule.getRuleType());
                continue;
            }

            switch (type) {
                case PERCENTAGE:
                    return matchPercentage(rule.getRuleValue(), safeParams);
                case BLACKLIST:
                    return !matchCollection(rule.getRuleValue(), safeParams);
                case WHITELIST:
                    return matchCollection(rule.getRuleValue(), safeParams);
                case EXPRESSION:
                    return matchExpression(rule.getRuleValue(), safeParams);
                default:
                    break;
            }
        }
        return false;
    }

    /**
     * 通过哈希稳定主体值到 [0, 99] 区间来匹配百分比规则。
     *
     * @param ruleValue 百分比文本
     * @param params    灰度参数
     * @return 匹配结果
     */
    private boolean matchPercentage(String ruleValue, Map<String, Object> params) {
        int percentage;
        try {
            percentage = Integer.parseInt(ruleValue);
        } catch (Exception ex) {
            log.warn("invalid percentage rule value, value={}", ruleValue);
            return false;
        }
        if (percentage <= 0) {
            return false;
        }
        if (percentage >= 100) {
            return true;
        }

        String subject = extractSubject(params);
        int bucket = Math.abs(subject.hashCode()) % 100;
        return bucket < percentage;
    }

    /**
     * 匹配由 JSON 数组表示的白名单或黑名单规则。
     *
     * @param ruleValue JSON 数组文本
     * @param params    灰度参数
     * @return 主体存在于集合中时返回 true
     */
    private boolean matchCollection(String ruleValue, Map<String, Object> params) {
        String subject = extractSubject(params);
        try {
            // 从缓存中获取JSONArray，避免重复解析
            JSONArray values = JSON_ARRAY_CACHE.computeIfAbsent(ruleValue, JSONArray::parseArray);
            // 直接使用JSONArray的contains方法，避免手动遍历
            return values.contains(subject);
        } catch (Exception ex) {
            log.warn("invalid list rule value, value={}", ruleValue);
        }
        return false;
    }

    /**
     * 使用 JMESPath 计算表达式规则。
     *
     * @param expressionText 表达式文本
     * @param params         灰度参数
     * @return 表达式结果
     */
    private boolean matchExpression(String expressionText, Map<String, Object> params) {
        try {
            // 从缓存中获取预编译的 JMESPath 表达式，避免重复编译
            Expression<JsonNode> expression = EXPRESSION_CACHE.computeIfAbsent(expressionText, PARSER::compile);

            // 将入参 Map 转换为 Jackson JsonNode
            JsonNode input = OBJECT_MAPPER.valueToTree(params);

            // 执行 JMESPath 搜索并转换为 boolean 结果
            JsonNode result = expression.search(input);
            return result != null && result.asBoolean();
        } catch (Exception ex) {
            log.warn("JMESPath rule evaluate failed, expression={}", expressionText, ex);
            return false;
        }
    }

    /**
     * 提取稳定主体标识，用于哈希和名单匹配。
     *
     * @param params 灰度参数
     * @return 主体文本
     */
    private String extractSubject(Map<String, Object> params) {
        // 按优先级顺序查找主题ID
        Object subject = params.get("userId");
        if (subject == null)
            subject = params.get("user_id");
        if (subject == null)
            subject = params.get("uid");
        if (subject == null)
            subject = params.get("id");

        return subject != null ? String.valueOf(subject) : params.toString();
    }
}
