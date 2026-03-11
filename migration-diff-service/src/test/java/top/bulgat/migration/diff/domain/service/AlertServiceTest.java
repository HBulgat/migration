package top.bulgat.migration.diff.domain.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import top.bulgat.common.notice.NoticeService;
import top.bulgat.common.notice.email.EmailMessage;
import top.bulgat.common.notice.feishu.FeishuTextMessage;
import top.bulgat.migration.diff.domain.model.AlertRule;
import top.bulgat.migration.diff.domain.model.AlertTemplate;
import top.bulgat.migration.diff.domain.model.DiffItem;
import top.bulgat.migration.diff.domain.model.DiffRequest;
import top.bulgat.migration.diff.domain.model.DiffResult;
import top.bulgat.migration.diff.domain.model.DiffType;
import top.bulgat.migration.diff.domain.repository.AlertRuleRepository;
import top.bulgat.migration.diff.domain.repository.AlertTemplateRepository;

class AlertServiceTest {

        private AlertRuleRepository alertRuleRepository;
        private AlertTemplateRepository alertTemplateRepository;
        private NoticeService noticeService;
        private AlertService alertService;

        @BeforeEach
        void setUp() {
                alertRuleRepository = Mockito.mock(AlertRuleRepository.class);
                alertTemplateRepository = Mockito.mock(AlertTemplateRepository.class);
                noticeService = Mockito.mock(NoticeService.class);
                alertService = new AlertService(alertRuleRepository, alertTemplateRepository, noticeService);
        }

        @Test
        void alertIfNeeded_shouldNotAlertWhenNoDiffAndBothSuccess() {
                DiffRequest request = createRequest("key1", true, true);
                DiffResult result = new DiffResult(false, List.of(), 5L);

                alertService.alertIfNeeded(request, result);

                verify(alertRuleRepository, never()).findEnabledRules(any());
                verify(noticeService, never()).send(any());
        }

        @Test
        void alertIfNeeded_shouldAlertWhenHasDiff() {
                DiffRequest request = createRequest("key1", true, true);
                DiffResult result = new DiffResult(true, List.of(
                                new DiffItem("$.price", "100", "200", DiffType.MODIFY)), 5L);

                AlertRule feishuRule = new AlertRule("key1", "飞书告警", true, "FEISHU",
                                "tpl1", List.of("https://hook1", "https://hook2"));
                when(alertRuleRepository.findEnabledRules("key1")).thenReturn(List.of(feishuRule));

                AlertTemplate template = new AlertTemplate("FEISHU", "test", "Alert: ${migrationKey} diff=${hasDiff}");
                when(alertTemplateRepository.findByTemplateKey("tpl1")).thenReturn(template);
                when(noticeService.send(any())).thenReturn(true);

                alertService.alertIfNeeded(request, result);

                verify(noticeService, times(2)).send(argThat(msg -> msg instanceof FeishuTextMessage
                                && ((FeishuTextMessage) msg).getText().contains("Alert: key1 diff=true")));
        }

        @Test
        void alertIfNeeded_shouldAlertWhenOldFailed() {
                DiffRequest request = createRequest("key1", false, true);
                DiffResult result = new DiffResult(false, List.of(), 5L);

                AlertRule rule = new AlertRule("key1", "飞书告警", true, "FEISHU",
                                "tpl1", List.of("https://hook1"));
                when(alertRuleRepository.findEnabledRules("key1")).thenReturn(List.of(rule));
                when(alertTemplateRepository.findByTemplateKey("tpl1")).thenReturn(null);
                when(noticeService.send(any())).thenReturn(true);

                alertService.alertIfNeeded(request, result);

                verify(noticeService, times(1)).send(any(FeishuTextMessage.class));
        }

        @Test
        void alertIfNeeded_shouldAlertWhenNewFailed() {
                DiffRequest request = createRequest("key1", true, false);
                DiffResult result = new DiffResult(false, List.of(), 5L);

                AlertRule rule = new AlertRule("key1", "邮件告警", true, "EMAIL",
                                "tpl_email", List.of("a@test.com", "b@test.com"));
                when(alertRuleRepository.findEnabledRules("key1")).thenReturn(List.of(rule));
                when(alertTemplateRepository.findByTemplateKey("tpl_email")).thenReturn(null);
                when(noticeService.send(any())).thenReturn(true);

                alertService.alertIfNeeded(request, result);

                verify(noticeService, times(1)).send(argThat(msg -> msg instanceof EmailMessage
                                && ((EmailMessage) msg).getTo().size() == 2));
        }

        @Test
        void alertIfNeeded_shouldNotThrowWhenSendFails() {
                DiffRequest request = createRequest("key1", true, true);
                DiffResult result = new DiffResult(true, List.of(), 5L);

                AlertRule rule = new AlertRule("key1", "飞书告警", true, "FEISHU",
                                "tpl1", List.of("https://hook1"));
                when(alertRuleRepository.findEnabledRules("key1")).thenReturn(List.of(rule));
                when(alertTemplateRepository.findByTemplateKey("tpl1")).thenReturn(null);
                when(noticeService.send(any())).thenThrow(new RuntimeException("send error"));

                // Should not throw
                alertService.alertIfNeeded(request, result);
        }

        @Test
        void alertIfNeeded_shouldNotAlertWhenNoRules() {
                DiffRequest request = createRequest("key1", true, true);
                DiffResult result = new DiffResult(true, List.of(), 5L);

                when(alertRuleRepository.findEnabledRules("key1")).thenReturn(List.of());

                alertService.alertIfNeeded(request, result);

                verify(noticeService, never()).send(any());
        }

        @Test
        void alertIfNeeded_shouldUseCustomTemplate() {
                DiffRequest request = createRequest("key1", true, true);
                DiffResult result = new DiffResult(true,
                                List.of(new DiffItem("$.name", "old", "new", DiffType.MODIFY)), 5L);

                AlertRule rule = new AlertRule("key1", "飞书", true, "FEISHU",
                                "custom_tpl", List.of("https://hook1"));
                when(alertRuleRepository.findEnabledRules("key1")).thenReturn(List.of(rule));

                AlertTemplate customTemplate = new AlertTemplate("FEISHU", "自定义",
                                "MK=${migrationKey} ITEMS=${diffItemCount}");
                when(alertTemplateRepository.findByTemplateKey("custom_tpl")).thenReturn(customTemplate);
                when(noticeService.send(any())).thenReturn(true);

                alertService.alertIfNeeded(request, result);

                verify(noticeService).send(argThat(msg -> msg instanceof FeishuTextMessage
                                && ((FeishuTextMessage) msg).getText().equals("MK=key1 ITEMS=1")));
        }

        private DiffRequest createRequest(String migrationKey, Boolean oldSuccess, Boolean newSuccess) {
                return new DiffRequest(
                                migrationKey, "trace-1",
                                "{\"a\":1}", "{\"a\":2}",
                                10, 12, "{}",
                                oldSuccess, newSuccess,
                                oldSuccess ? null : "OldError",
                                newSuccess ? null : "NewError",
                                null, null,
                                4, null,
                                true, false);
        }
}
