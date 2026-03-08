package top.bulgat.migration.diff.domain.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import top.bulgat.common.notice.NoticeService;
import top.bulgat.common.notice.email.EmailMessage;
import top.bulgat.common.notice.feishu.FeishuTextMessage;
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
public class AlertService {

    private static final Logger log = LoggerFactory.getLogger(AlertService.class);

    private static final String DEFAULT_TEMPLATE = "🚨 迁移告警 [${migrationKey}]\nTraceID: ${traceId}\nhasDiff: ${hasDiff}\n"
            + "差异项数: ${diffItemCount}\n旧接口: ${oldSuccess} (${oldCostTimeMs}ms)\n"
            + "新接口: ${newSuccess} (${newCostTimeMs}ms)\n${errorSummary}";
    private static final String DEFAULT_EMAIL_SUBJECT = "[Migration Alert] ${migrationKey}";

    private final AlertRuleRepository alertRuleRepository;
    private final AlertTemplateRepository alertTemplateRepository;
    private final NoticeService noticeService;

    public AlertService(
            AlertRuleRepository alertRuleRepository,
            AlertTemplateRepository alertTemplateRepository,
            NoticeService noticeService) {
        this.alertRuleRepository = alertRuleRepository;
        this.alertTemplateRepository = alertTemplateRepository;
        this.noticeService = noticeService;
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
        AlertTemplate template = alertTemplateRepository.findByKey(rule.templateKey());
        String content = renderTemplate(
                template != null ? template.template() : DEFAULT_TEMPLATE,
                variables);

        switch (rule.channel()) {
            case "FEISHU" -> sendFeishu(rule, content);
            case "EMAIL" -> sendEmail(rule, content, variables);
            default -> log.warn("unsupported alert channel: {}", rule.channel());
        }
    }

    private void sendFeishu(AlertRule rule, String content) {
        for (String webhookUrl : rule.receivers()) {
            try {
                noticeService.send(FeishuTextMessage.builder()
                        .webhookUrl(webhookUrl)
                        .text(content)
                        .build());
            } catch (Exception ex) {
                log.warn("feishu alert failed, rule={}, webhook={}",
                        rule.name(), webhookUrl, ex);
            }
        }
    }

    private void sendEmail(AlertRule rule, String content, Map<String, String> variables) {
        try {
            String subject = renderTemplate(DEFAULT_EMAIL_SUBJECT, variables);
            noticeService.send(EmailMessage.builder()
                    .to(rule.receivers())
                    .subject(subject)
                    .body(content)
                    .build());
        } catch (Exception ex) {
            log.warn("email alert failed, rule={}", rule.name(), ex);
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
        vars.put("MigrationTaskStatus", str(request.getMigrationTaskStatus()));
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
