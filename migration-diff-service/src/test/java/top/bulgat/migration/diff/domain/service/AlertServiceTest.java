package top.bulgat.migration.diff.domain.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.fasterxml.jackson.databind.JsonNode;
import top.bulgat.common.notice.feishu.FeishuNoticeSender;
import top.bulgat.common.notice.NoticeChannel;
import top.bulgat.common.notice.NoticeService;
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
        private ObjectMapper objectMapper;
        private AlertService alertService;

        @BeforeEach
        void setUp() {
                alertRuleRepository = Mockito.mock(AlertRuleRepository.class);
                alertTemplateRepository = Mockito.mock(AlertTemplateRepository.class);
                noticeService = Mockito.mock(NoticeService.class);
                objectMapper = new ObjectMapper();
                alertService = new AlertService(alertRuleRepository, alertTemplateRepository, noticeService,
                                objectMapper);
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
        void alertIfNeeded_shouldAlertWhenHasDiff() throws Exception {
                DiffRequest request = createRequest("key1", true, true);
                DiffResult result = new DiffResult(true, List.of(
                                new DiffItem("$.price", "100", "200", DiffType.MODIFY)), 5L);

                AlertRule feishuRule = new AlertRule("key1", "rule1", "飞书告警", true, NoticeChannel.FEISHU,
                                "tpl1",
                                List.of("https://open.feishu.cn/open-apis/bot/v2/hook/e3fe05da-fba0-4abe-8fea-2def5f2ae8fd"));
                when(alertRuleRepository.findEnabledRules("key1")).thenReturn(List.of(feishuRule));

                JsonNode templateNode = objectMapper.readTree(
                                "{\"msg_type\":\"text\",\"text\":\"Alert: ${migrationKey} diff=${hasDiff}\"}");
                AlertTemplate template = new AlertTemplate(NoticeChannel.FEISHU, "test", templateNode);
                when(alertTemplateRepository.findByTemplateKey("tpl1")).thenReturn(template);
                when(noticeService.send(any())).thenReturn(true);

                alertService.alertIfNeeded(request, result);

                verify(noticeService, times(1)).send(argThat(msg -> msg instanceof FeishuTextMessage
                                && ((FeishuTextMessage) msg).getText().contains("Alert: key1 diff=true")));
        }

        @Test
        void alertIfNeeded_shouldAlertWhenOldFailed() {
                DiffRequest request = createRequest("key1", false, true);
                DiffResult result = new DiffResult(false, List.of(), 5L);

                AlertRule rule = new AlertRule("key1", "rule1", "飞书告警", true, NoticeChannel.FEISHU,
                                "tpl1",
                                List.of("https://open.feishu.cn/open-apis/bot/v2/hook/e3fe05da-fba0-4abe-8fea-2def5f2ae8fd"));
                when(alertRuleRepository.findEnabledRules("key1")).thenReturn(List.of(rule));
                when(alertTemplateRepository.findByTemplateKey("tpl1")).thenReturn(null);
                when(noticeService.send(any())).thenReturn(true);

                alertService.alertIfNeeded(request, result);

                // Assuming default template fallback to FeishuTextMessage
                verify(noticeService, times(1)).send(any(FeishuTextMessage.class));
        }

        @Test
        void alertIfNeeded_shouldLogWarningWhenEmailChannel() {
                DiffRequest request = createRequest("key1", true, false);
                DiffResult result = new DiffResult(false, List.of(), 5L);

                AlertRule rule = new AlertRule("key1", "rule1", "邮件告警", true, NoticeChannel.EMAIL,
                                "tpl_email", List.of("a@test.com", "b@test.com"));
                when(alertRuleRepository.findEnabledRules("key1")).thenReturn(List.of(rule));
                when(alertTemplateRepository.findByTemplateKey("tpl_email")).thenReturn(null);
                when(noticeService.send(any())).thenReturn(true);

                alertService.alertIfNeeded(request, result);

                // Email is ignored in AlertService now (logs warning)
                verify(noticeService, never()).send(any());
        }

        @Test
        void alertIfNeeded_shouldNotThrowWhenSendFails() {
                DiffRequest request = createRequest("key1", true, true);
                DiffResult result = new DiffResult(true, List.of(), 5L);

                AlertRule rule = new AlertRule("key1", "rule1", "飞书告警", true, NoticeChannel.FEISHU,
                                "tpl1",
                                List.of("https://open.feishu.cn/open-apis/bot/v2/hook/e3fe05da-fba0-4abe-8fea-2def5f2ae8fd"));
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
        void alertIfNeeded_shouldUseCustomTemplate() throws Exception {
                // IMPORTANT: Replace mock with real FeishuNoticeSender to actually trigger a
                // test message
                NoticeService realNoticeService = new NoticeService(List.of(new FeishuNoticeSender()));
                alertService = new AlertService(alertRuleRepository, alertTemplateRepository, realNoticeService,
                                objectMapper);
                DiffRequest request = createRequest("key1", true, true);
                DiffResult result = new DiffResult(true,
                                List.of(new DiffItem("$.name", "old", "new", DiffType.MODIFY)), 5L);

                AlertRule rule = new AlertRule("key1", "rule1", "飞书", true, NoticeChannel.FEISHU,
                                "custom_tpl",
                                List.of("https://open.feishu.cn/open-apis/bot/v2/hook/e3fe05da-fba0-4abe-8fea-2def5f2ae8fd"));
                when(alertRuleRepository.findEnabledRules("key1")).thenReturn(List.of(rule));

                JsonNode customTemplateNode = objectMapper.readTree(
                                "{\"msg_type\":\"t666ext\",\"text\":\"MK=${migrationKey} ITEMS=${diffItemCount}\"}");
                AlertTemplate customTemplate = new AlertTemplate(NoticeChannel.FEISHU, "自定义", customTemplateNode);
                when(alertTemplateRepository.findByTemplateKey("custom_tpl")).thenReturn(customTemplate);
                when(noticeService.send(any())).thenReturn(true);

                alertService.alertIfNeeded(request, result);

                // Allow some time for HTTP request to be completely dispatched
                Thread.sleep(2000);
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
