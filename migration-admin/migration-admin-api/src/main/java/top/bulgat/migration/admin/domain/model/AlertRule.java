package top.bulgat.migration.admin.domain.model;

import top.bulgat.common.notice.NoticeChannel;

import java.util.List;

import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 告警规则模型（充血实体）。
 * <p>
 * 定义"往哪发"：渠道、接收人、开关，以及引用的模板。
 */
@Getter
public class AlertRule {
    
    private final String migrationKey;
    private final String ruleId;
    private String name;
    private boolean enable;
    private NoticeChannel channel;
    private String templateKey;
    private List<String> receivers;
    private final LocalDateTime createTime;
    private LocalDateTime updateTime;

    public AlertRule(String migrationKey, String ruleId, String name, boolean enable,
                     NoticeChannel channel, String templateKey, List<String> receivers,
                     LocalDateTime createTime, LocalDateTime updateTime) {
        this.migrationKey = migrationKey;
        this.ruleId = ruleId;
        this.name = name;
        this.enable = enable;
        this.channel = channel;
        this.templateKey = templateKey;
        this.receivers = receivers;
        this.createTime = createTime;
        this.updateTime = updateTime;
    }

    public void update(String name, NoticeChannel channel, String templateKey, List<String> receivers, Boolean enable) {
        boolean changed = false;
        if (name != null && !name.equals(this.name)) {
            this.name = name;
            changed = true;
        }
        if (channel != null && this.channel != channel) {
            this.channel = channel;
            changed = true;
        }
        if (templateKey != null && !templateKey.equals(this.templateKey)) {
            this.templateKey = templateKey;
            changed = true;
        }
        if (receivers != null && !receivers.equals(this.receivers)) {
            this.receivers = receivers;
            changed = true;
        }
        if (enable != null && this.enable != enable) {
            this.enable = enable;
            changed = true;
        }
        if (changed) {
            this.updateTime = LocalDateTime.now();
        }
    }

    public void changeEnable(boolean enable) {
        if (this.enable != enable) {
            this.enable = enable;
            this.updateTime = LocalDateTime.now();
        }
    }
}
