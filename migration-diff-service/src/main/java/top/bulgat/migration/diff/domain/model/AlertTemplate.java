package top.bulgat.migration.diff.domain.model;

import lombok.AllArgsConstructor;

/**
 * 告警消息模板模型。
 * <p>
 * 模板是全局共享的，多个 migration_key 的规则可通过 template_key 引用同一套模板。
 * 模板内容支持 {@code ${variable}} 占位符。
 *
 * @param channel  渠道类型：FEISHU / EMAIL。
 * @param name     模板名称。
 * @param template 模板内容，支持 ${variable} 变量占位符。
 */
public record AlertTemplate(String channel, String name, String template) {
}
