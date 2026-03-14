package top.bulgat.migration.admin.domain.model;

import top.bulgat.common.notice.NoticeChannel;
import com.fasterxml.jackson.databind.JsonNode;

import java.time.LocalDateTime;

/**
 * 告警消息模板模型。
 * <p>
 * 模板是全局共享的，多个 migration_key 的规则可通过 template_key 引用同一套模板。
 * 模板内容支持 {@code ${variable}} 占位符。
 */
public class AlertTemplate {

    private String templateKey;
    private NoticeChannel channel;
    private String name;
    private JsonNode template;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public AlertTemplate(String templateKey, NoticeChannel channel, String name, JsonNode template, LocalDateTime createTime, LocalDateTime updateTime) {
        this.templateKey = templateKey;
        this.channel = channel;
        this.name = name;
        this.template = template;
        this.createTime = createTime;
        this.updateTime = updateTime;
    }

    public AlertTemplate(NoticeChannel channel, String name, JsonNode template) {
        this.channel = channel;
        this.name = name;
        this.template = template;
    }

    public void update(NoticeChannel channel, String name, JsonNode template) {
        this.channel = channel;
        this.name = name;
        this.template = template;
        this.updateTime = LocalDateTime.now();
    }

    public void initCreateTime() {
        this.createTime = LocalDateTime.now();
        this.updateTime = this.createTime;
    }

    public void initTemplateKey(String templateKey) {
        this.templateKey = templateKey;
    }

    // Getters
    public String getTemplateKey() { return templateKey; }
    public NoticeChannel getChannel() { return channel; }
    public String getName() { return name; }
    public JsonNode getTemplate() { return template; }
    public LocalDateTime getCreateTime() { return createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
}
