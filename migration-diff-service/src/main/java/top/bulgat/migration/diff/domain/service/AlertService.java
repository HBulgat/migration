package top.bulgat.migration.diff.domain.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import top.bulgat.common.notice.NoticeService;
import top.bulgat.common.notice.feishu.FeishuMessage;
import top.bulgat.migration.diff.domain.model.AlertRule;
import top.bulgat.migration.diff.domain.model.AlertTemplate;
import top.bulgat.migration.diff.domain.model.DiffItem;
import top.bulgat.migration.diff.domain.model.DiffRequest;
import top.bulgat.migration.diff.domain.model.DiffResult;
import top.bulgat.migration.diff.domain.repository.AlertRuleRepository;
import top.bulgat.migration.diff.domain.repository.AlertTemplateRepository;

/**
 * 告警服务。
 * <p>
 * 在 Diff 执行完成后，判断是否需要告警，加载告警规则和模板，渲染变量后发送通知。
 */
@Service
@Slf4j
public class AlertService {


    private static final String DEFAULT_TEMPLATE_STR = "{\"msg_type\":\"text\",\"text\":\"🚨 迁移告警 [${migrationKey}]\\nTraceID: ${traceId}\\n差异项数: ${diffItemCount}\\n旧接口: ${oldSuccess}\\n新接口: ${newSuccess}\\n异常汇总: ${errorSummary}\"}";

    private final AlertRuleRepository alertRuleRepository;
    private final AlertTemplateRepository alertTemplateRepository;
    private final NoticeService noticeService;
    private final ObjectMapper objectMapper;

    public AlertService(
            AlertRuleRepository alertRuleRepository,
            AlertTemplateRepository alertTemplateRepository,
            NoticeService noticeService,
            ObjectMapper objectMapper) {
        this.alertRuleRepository = alertRuleRepository;
        this.alertTemplateRepository = alertTemplateRepository;
        this.noticeService = noticeService;
        this.objectMapper = objectMapper;
    }

    /**
     * 判定是否需要告警，如需则加载规则和模板并发送通知。
     * <p>
     * 本方法不会抛出异常，所有异常均在内部捕获并记录日志。
     *
     * @param request Diff 请求上下文
     * @param result  Diff 比对结果
     */
    public void alertIfNeeded(DiffRequest request, DiffResult result) {
        try {
            doAlertIfNeeded(request, result);
        } catch (Exception ex) {
            log.warn("alert processing failed, migrationKey={}", request.getMigrationKey(), ex);
        }
    }

    private void doAlertIfNeeded(DiffRequest request, DiffResult result) {
        boolean shouldAlert = result.hasDiff()
                || Boolean.FALSE.equals(request.getOldSuccess())
                || Boolean.FALSE.equals(request.getNewSuccess());

        if (!shouldAlert) {
            return;
        }

        List<AlertRule> rules = alertRuleRepository.findEnabledRules(request.getMigrationKey());
        if (rules.isEmpty()) {
            return;
        }

        Map<String, String> variables = buildVariables(request, result);
        log.info("alert.triggered migrationKey={}, ruleCount={}", request.getMigrationKey(), rules.size());

        for (AlertRule rule : rules) {
            try {
                sendAlert(rule, variables);
            } catch (Exception ex) {
                log.warn("alert send failed, migrationKey={}, rule={}",
                        request.getMigrationKey(), rule.name(), ex);
            }
        }
    }

    private void sendAlert(AlertRule rule, Map<String, String> variables) {
        AlertTemplate template = alertTemplateRepository.findByTemplateKey(rule.templateKey());

        switch (rule.channel()) {
            case FEISHU -> sendFeishu(rule, template, variables);
            case EMAIL -> log.warn("Email alert channel is not supported yet.");
            default -> log.warn("unsupported alert channel: {}", rule.channel());
        }
    }

    private void sendFeishu(AlertRule rule, AlertTemplate template, Map<String, String> variables) {
        try {
            String rawJsonStr;
            if (template != null && template.template() != null) {
                rawJsonStr = objectMapper.writeValueAsString(template.template());
            } else {
                rawJsonStr = DEFAULT_TEMPLATE_STR;
            }

            String renderedJsonStr = renderTemplate(rawJsonStr, variables);
            FeishuMessage feishuMsg = objectMapper.readValue(renderedJsonStr, FeishuMessage.class);

            for (String webhookUrl : rule.receivers()) {
                feishuMsg.setWebhookUrl(webhookUrl);
                try {
                    noticeService.send(feishuMsg);
                } catch (Exception ex) {
                    log.warn("feishu alert failed, rule={}, webhook={}", rule.name(), webhookUrl, ex);
                }
            }
        } catch (Exception e) {
            log.error("Failed to parse and send Feishu message, rule={}", rule.name(), e);
        }
    }

    /**
     * 构建模板变量上下文。
     */
    private Map<String, String> buildVariables(DiffRequest request, DiffResult result) {
        Map<String, String> vars = new LinkedHashMap<>();
        vars.put("migrationKey", str(request.getMigrationKey()));
        vars.put("traceId", str(request.getTraceId()));
        vars.put("hasDiff", String.valueOf(result.hasDiff()));
        vars.put("diffItemCount", String.valueOf(result.getDiffItems().size()));
        vars.put("diffDetails", formatDiffDetails(result));
        vars.put("oldSuccess", str(request.getOldSuccess()));
        vars.put("newSuccess", str(request.getNewSuccess()));
        vars.put("oldCostTimeMs", str(request.getOldCostTimeMs()));
        vars.put("newCostTimeMs", str(request.getNewCostTimeMs()));
        vars.put("migrationStatus", str(request.getMigrationTaskStatus()));
        vars.put("grayscaleHit", str(request.getGrayscaleHit()));
        vars.put("errorSummary", buildErrorSummary(request));
        return vars;
    }

    /**
     * 将模板中的 ${variable} 占位符替换为实际值。
     */
    private String renderTemplate(String template, Map<String, String> variables) {
        String rendered = template;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            rendered = rendered.replace("${" + entry.getKey() + "}", entry.getValue());
        }
        return rendered;
    }

    private String formatDiffDetails(DiffResult result) {
        if (result.getDiffItems().isEmpty()) {
            return "无差异明细";
        }
        return result.getDiffItems().stream()
                .map(this::formatDiffItem)
                .collect(Collectors.joining("\n"));
    }

    private String formatDiffItem(DiffItem item) {
        return item.fieldPath() + " [" + item.diffType() + "]: "
                + truncate(item.oldValue(), 100) + " → " + truncate(item.newValue(), 100);
    }

    private String buildErrorSummary(DiffRequest request) {
        StringBuilder sb = new StringBuilder();
        if (Boolean.FALSE.equals(request.getOldSuccess())) {
            sb.append("旧接口异常: ").append(str(request.getOldErrorMessage()));
        } else {
            sb.append("旧接口正常");
        }
        sb.append("; ");
        if (Boolean.FALSE.equals(request.getNewSuccess())) {
            sb.append("新接口异常: ").append(str(request.getNewErrorMessage()));
        } else {
            sb.append("新接口正常");
        }
        return sb.toString();
    }

    private static String str(Object value) {
        return value != null ? String.valueOf(value) : "N/A";
    }

    private static String truncate(String value, int maxLen) {
        if (value == null) {
            return "null";
        }
        return value.length() <= maxLen ? value : value.substring(0, maxLen) + "...";
    }
}
