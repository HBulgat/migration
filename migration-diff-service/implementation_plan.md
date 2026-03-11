# 告警功能实现方案

## 需求摘要

Diff Service 执行比对后，若发现异常（Diff 不一致 / 接口调用失败），根据该 `migration_key` 的告警规则发送通知。

**核心约束**：告警规则按 `migration_key` 粒度 | 模板全局复用 | 存 Nacos | 渠道支持飞书 Webhook + 邮件 | 使用 `common-notice` 依赖。

---

## 告警触发条件

`hasDiff=true` 或 `oldSuccess=false` 或 `newSuccess=false` → 触发告警。两端都成功且无差异 → 不告警。

---

## Nacos 配置设计

### 配置 1：告警规则（按 migration_key）

dataId: `alert_migration_{key}`，group: `ALERT_RULE_GROUP`

参考 [alter_migration_{key}.json](file:///Users/bytedance/IdeaProjects/migration/migration-diff-service/conf/alter_migration_%7Bkey%7D.json)：

```json
[
    {
        "migration_key": "test_migration",
        "name": "飞书告警配置1",
        "enable": true,
        "channel": "FEISHU",
        "template_key": "template_feishu_key",
        "receivers": ["webhook1", "webhook2"]
    },
    {
        "migration_key": "test_migration",
        "name": "邮件告警配置",
        "enable": true,
        "channel": "EMAIL",
        "template_key": "template_email_key",
        "receivers": ["admin@example.com", "dev@example.com"]
    }
]
```

| 字段 | 说明 |
|------|------|
| `migration_key` | 所属迁移任务标识 |
| `name` | 告警规则名称（便于管理辨识） |
| `enable` | 是否启用 |
| [channel](file:///Users/bytedance/IdeaProjects/common/common-notice/src/main/java/top/bulgat/common/notice/email/EmailNoticeSender.java#34-38) | 渠道类型：`FEISHU` / `EMAIL` |
| `template_key` | 引用的模板 key |
| `receivers` | 接收人列表（飞书为 webhook URL，邮件为邮箱地址） |

### 配置 2：告警模板（全局，跨 migration_key 复用）

dataId: `alert_template`，group: `ALERT_RULE_GROUP`

参考 [alter_template.json](file:///Users/bytedance/IdeaProjects/migration/migration-diff-service/conf/alter_template.json)：

```json
{
    "template_feishu_key": {
        "channel": "FEISHU",
        "name": "飞书默认告警模板",
        "template": "🚨 迁移告警 [${migrationKey}]\nTraceID: ${traceId}\n差异项数: ${diffItemCount}\n旧接口: ${oldSuccess} (${oldCostTimeMs}ms)\n新接口: ${newSuccess} (${newCostTimeMs}ms)\n${errorSummary}"
    },
    "template_email_key": {
        "channel": "EMAIL",
        "name": "邮件默认告警模板",
        "template": "🚨 迁移告警 [${migrationKey}]\nTraceID: ${traceId}\n差异项数: ${diffItemCount}\n旧接口: ${oldSuccess} (${oldCostTimeMs}ms)\n新接口: ${newSuccess} (${newCostTimeMs}ms)\n${errorSummary}"
    }
}
```

> [!NOTE]
> 模板是**全局共享**的，多个 migration_key 的规则可以通过 `template_key` 引用同一套模板。

### 可用模板变量

| 变量名 | 说明 | 示例值 |
|--------|------|--------|
| `migrationKey` | 迁移任务标识 | `user-getUser-api` |
| `traceId` | 链路追踪 ID | `abc-123-def` |
| [hasDiff](file:///Users/bytedance/IdeaProjects/migration/migration-diff-service/src/main/java/top/bulgat/migration/diff/domain/model/DiffResult.java#18-24) | 是否有差异 | `true` |
| `diffItemCount` | 差异项数量 | `3` |
| `diffDetails` | 差异明细 | `data.price: 100.0 → 100.5` |
| `oldSuccess` | 旧接口是否成功 | `true` |
| `newSuccess` | 新接口是否成功 | `false` |
| `oldCostTimeMs` | 旧接口耗时(ms) | `12` |
| `newCostTimeMs` | 新接口耗时(ms) | `45` |
| `migrationStatus` | 迁移阶段(1-7) | `4` |
| `grayscaleHit` | 是否命中灰度 | `true` |
| `errorSummary` | 错误摘要 | `旧接口正常; 新接口异常: NPE` |

---

## Proposed Changes

### Diff Service — 依赖引入

#### [MODIFY] [pom.xml](file:///Users/bytedance/IdeaProjects/migration/migration-diff-service/pom.xml)

新增 `common-notice`、`okhttp3`、`javax.mail` 依赖。

---

### Diff Service — 领域模型

#### [NEW] [AlertRule.java](file:///Users/bytedance/IdeaProjects/migration/migration-diff-service/src/main/java/top/bulgat/migration/diff/domain/model/AlertRule.java)

```java
public class AlertRule {
    private String migrationKey;
    private String name;           // 规则名称
    private boolean enable;
    private String channel;        // "FEISHU" / "EMAIL"
    private String templateKey;    // 引用模板的 key
    private List<String> receivers; // 飞书=webhook URL，邮件=邮箱地址
}
```

#### [NEW] [AlertTemplate.java](file:///Users/bytedance/IdeaProjects/migration/migration-diff-service/src/main/java/top/bulgat/migration/diff/domain/model/AlertTemplate.java)

```java
public class AlertTemplate {
    private String channel;   // "FEISHU" / "EMAIL"
    private String name;      // 模板名称
    private String template;  // 模板内容，支持 ${variable}
}
```

---

### Diff Service — 仓储接口

#### [NEW] [AlertRuleRepository.java](file:///Users/bytedance/IdeaProjects/migration/migration-diff-service/src/main/java/top/bulgat/migration/diff/domain/repository/AlertRuleRepository.java)

```java
public interface AlertRuleRepository {
    List<AlertRule> findEnabledRules(String migrationKey);
}
```

#### [NEW] [AlertTemplateRepository.java](file:///Users/bytedance/IdeaProjects/migration/migration-diff-service/src/main/java/top/bulgat/migration/diff/domain/repository/AlertTemplateRepository.java)

```java
public interface AlertTemplateRepository {
    /** 根据 templateKey 查找模板，未找到返回 null */
    AlertTemplate findByKey(String templateKey);
}
```

---

### Diff Service — 基础设施层

#### [NEW] [NacosAlertRuleRepository.java](file:///Users/bytedance/IdeaProjects/migration/migration-diff-service/src/main/java/top/bulgat/migration/diff/infrastructure/configcenter/NacosAlertRuleRepository.java)

从 Nacos 读取 `alert_migration_{key}`，解析 JSON 数组，过滤 `enable=true` 的规则。

#### [NEW] [NacosAlertTemplateRepository.java](file:///Users/bytedance/IdeaProjects/migration/migration-diff-service/src/main/java/top/bulgat/migration/diff/infrastructure/configcenter/NacosAlertTemplateRepository.java)

从 Nacos 读取全局 `alert_template`，解析为 `Map<String, AlertTemplate>`，按 `templateKey` 查找。

---

### Diff Service — 告警服务

#### [NEW] [AlertService.java](file:///Users/bytedance/IdeaProjects/migration/migration-diff-service/src/main/java/top/bulgat/migration/diff/domain/service/AlertService.java)

```java
@Service
public class AlertService {

    private final AlertRuleRepository alertRuleRepository;
    private final AlertTemplateRepository alertTemplateRepository;
    private final NoticeService noticeService;

    public void alertIfNeeded(DiffRequest request, DiffResult result) {
        boolean shouldAlert = result.hasDiff()
            || Boolean.FALSE.equals(request.oldSuccess())
            || Boolean.FALSE.equals(request.newSuccess());
        if (!shouldAlert) return;

        List<AlertRule> rules = alertRuleRepository.findEnabledRules(request.migrationKey());
        if (rules.isEmpty()) return;

        Map<String, String> variables = buildVariables(request, result);

        for (AlertRule rule : rules) {
            try {
                AlertTemplate template = alertTemplateRepository.findByKey(rule.getTemplateKey());
                String content = renderTemplate(
                    template != null ? template.getTemplate() : DEFAULT_TEMPLATE,
                    variables);

                for (String receiver : rule.getReceivers()) {
                    switch (rule.getChannel()) {
                        case "FEISHU" -> noticeService.send(FeishuTextMessage.builder()
                            .webhookUrl(receiver)
                            .text(content)
                            .build());
                        case "EMAIL" -> noticeService.send(EmailMessage.builder()
                            .to(List.of(receiver))
                            .subject("[Migration Alert] " + request.migrationKey())
                            .body(content)
                            .build());
                    }
                }
            } catch (Exception ex) {
                log.warn("alert send failed, rule={}", rule.getName(), ex);
            }
        }
    }
}
```

> [!NOTE]
> 飞书渠道：遍历 `receivers`（每个都是 webhook URL），逐个发送。
> 邮件渠道：`receivers` 是邮箱地址列表，可以一次发送给所有收件人或逐个发送。

---

### Diff Service — 配置类

#### [NEW] [AlertNoticeAutoConfiguration.java](file:///Users/bytedance/IdeaProjects/migration/migration-diff-service/src/main/java/top/bulgat/migration/diff/infrastructure/config/AlertNoticeAutoConfiguration.java)

注册 [NoticeService](file:///Users/bytedance/IdeaProjects/common/common-notice/src/main/java/top/bulgat/common/notice/NoticeService.java#24-57) Bean（`FeishuNoticeSender` + 可选的 [EmailNoticeSender](file:///Users/bytedance/IdeaProjects/common/common-notice/src/main/java/top/bulgat/common/notice/email/EmailNoticeSender.java#17-65)）。

---

### Diff Service — 应用层集成

#### [MODIFY] [DiffApplicationService.java](file:///Users/bytedance/IdeaProjects/migration/migration-diff-service/src/main/java/top/bulgat/migration/diff/application/service/DiffApplicationService.java)

```diff
 DiffResult result = domainService.execute(request, rules);
 diffRecordRepository.save(request, result);
+alertService.alertIfNeeded(request, result);
 return result;
```

---

## 变更文件清单

| 文件 | 操作 |
|------|------|
| [pom.xml](file:///Users/bytedance/IdeaProjects/migration/pom.xml) | MODIFY — 新增依赖 |
| `domain/model/AlertRule.java` | NEW |
| `domain/model/AlertTemplate.java` | NEW |
| `domain/repository/AlertRuleRepository.java` | NEW |
| `domain/repository/AlertTemplateRepository.java` | NEW |
| `domain/service/AlertService.java` | NEW |
| `infrastructure/configcenter/NacosAlertRuleRepository.java` | NEW |
| `infrastructure/configcenter/NacosAlertTemplateRepository.java` | NEW |
| `infrastructure/config/AlertNoticeAutoConfiguration.java` | NEW |
| [application/service/DiffApplicationService.java](file:///Users/bytedance/IdeaProjects/migration/migration-diff-service/src/main/java/top/bulgat/migration/diff/application/service/DiffApplicationService.java) | MODIFY |

---

## Verification Plan

### Automated Tests

1. **`AlertServiceTest`** — 告警判定 + 模板渲染 + receivers 遍历
2. **`NacosAlertRuleRepositoryTest`** — 规则 JSON 解析
3. **`NacosAlertTemplateRepositoryTest`** — 模板 JSON 解析 + key 查找
4. **[DiffApplicationServiceTest](file:///Users/bytedance/IdeaProjects/migration/migration-diff-service/src/test/java/top/bulgat/migration/diff/application/service/DiffApplicationServiceTest.java#26-238)** — 验证 alertIfNeeded 被调用

```bash
mvn test -pl migration-diff-service -am
```
