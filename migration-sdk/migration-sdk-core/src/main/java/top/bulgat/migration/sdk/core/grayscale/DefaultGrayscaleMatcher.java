package top.bulgat.migration.sdk.core.grayscale;

import com.alibaba.fastjson2.JSONArray;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import top.bulgat.migration.sdk.core.model.GrayscaleConfig;
import top.bulgat.migration.sdk.core.model.GrayscaleRuleType;
import top.bulgat.migration.sdk.core.spi.GrayscaleMatcher;

/**
 * Default grayscale matcher supporting percentage, lists and expression rules.
 */
public class DefaultGrayscaleMatcher implements GrayscaleMatcher {

    private static final Logger log = LoggerFactory.getLogger(DefaultGrayscaleMatcher.class);
    private static final ExpressionParser PARSER = new SpelExpressionParser();
    private static final Map<String, Expression> EXPRESSION_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, JSONArray> JSON_ARRAY_CACHE = new ConcurrentHashMap<>();

    /**
     * Evaluates grayscale rules in order and returns on first decision.
     *
     * @param rules grayscale rules
     * @param params grayscale parameters
     * @return true when request hits grayscale
     */
    @Override
    public boolean match(List<GrayscaleConfig> rules, Map<String, Object> params) {
        if (rules == null || rules.isEmpty()) {
            return false;
        }

        Map<String, Object> safeParams = params == null ? Collections.emptyMap() : params;
        for (GrayscaleConfig rule : rules) {
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
     * Matches percentage rule by hashing stable subject into [0, 99].
     *
     * @param ruleValue percentage text
     * @param params grayscale parameters
     * @return match result
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
     * Matches whitelist/blacklist rule represented by JSON array.
     *
     * @param ruleValue json array text
     * @param params grayscale parameters
     * @return true when subject exists in collection
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
     * Evaluates expression rule using SpEL.
     *
     * @param expressionText expression text
     * @param params grayscale parameters
     * @return expression result
     */
    private boolean matchExpression(String expressionText, Map<String, Object> params) {
        try {
            // 从缓存中获取表达式，避免重复解析
            Expression expression = EXPRESSION_CACHE.computeIfAbsent(expressionText, PARSER::parseExpression);
            
            StandardEvaluationContext context = new StandardEvaluationContext();
            params.forEach(context::setVariable);
            context.setVariable("param", params);
            
            Boolean matched = expression.getValue(context, Boolean.class);
            return Boolean.TRUE.equals(matched);
        } catch (Exception ex) {
            log.warn("expression rule evaluate failed, expression={}", expressionText, ex);
            return false;
        }
    }

    /**
     * Extracts stable subject id for hashing and list matching.
     *
     * @param params grayscale parameters
     * @return subject text
     */
    private String extractSubject(Map<String, Object> params) {
        // 按优先级顺序查找主题ID
        Object subject = params.get("userId");
        if (subject == null) subject = params.get("user_id");
        if (subject == null) subject = params.get("uid");
        if (subject == null) subject = params.get("id");
        
        return subject != null ? String.valueOf(subject) : params.toString();
    }
}
