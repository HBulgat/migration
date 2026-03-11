package top.bulgat.migration.admin.domain.model;

import top.bulgat.common.notice.NoticeChannel;

import java.util.List;

/**
 * 告警规则模型。
 * <p>
 * 定义"往哪发"：渠道、接收人、开关，以及引用的模板。
 *
 * @param migrationKey 所属迁移任务标识。
 * @param name         告警规则名称。
 * @param enable       是否启用。
 * @param channel      渠道类型：FEISHU / EMAIL。
 * @param templateKey  引用的模板 key。
 * @param receivers    接收人列表。
 *                     <p>
 *                     飞书渠道是 webhook URL，邮件渠道是邮箱地址。
 */
public record AlertRule(String migrationKey, String name, boolean enable,
                        NoticeChannel channel, String templateKey,
                        List<String> receivers) {
}
