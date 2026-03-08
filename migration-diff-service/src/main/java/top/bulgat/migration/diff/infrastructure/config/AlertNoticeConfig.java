package top.bulgat.migration.diff.infrastructure.config;

import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.config.ConfigService;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import top.bulgat.common.notice.NoticeSender;
import top.bulgat.common.notice.NoticeService;
import top.bulgat.common.notice.email.EmailNoticeSender;
import top.bulgat.common.notice.feishu.FeishuNoticeSender;

/**
 * 告警通知基础设施自动装配。
 * <p>
 * 提供全局共享的 Nacos {@link ConfigService} Bean 和告警用 {@link NoticeService} Bean。
 */
@Configuration
public class AlertNoticeConfig {

    private static final Logger log = LoggerFactory.getLogger(AlertNoticeConfig.class);

    /**
     * 提供全局共享的 Nacos ConfigService Bean。
     */
    @Bean
    public ConfigService nacosConfigService(
            @Value("${migration.nacos.server-addr:localhost:8848}") String serverAddr,
            @Value("${migration.nacos.namespace:}") String namespace,
            @Value("${migration.nacos.username:}") String username,
            @Value("${migration.nacos.password:}") String password) throws Exception {
        Properties properties = new Properties();
        properties.setProperty("serverAddr", serverAddr);
        if (namespace != null && !namespace.isBlank()) {
            properties.setProperty("namespace", namespace);
        }
        if (username != null && !username.isBlank()) {
            properties.setProperty("username", username);
        }
        if (password != null && !password.isBlank()) {
            properties.setProperty("password", password);
        }
        return NacosFactory.createConfigService(properties);
    }

    /**
     * 注册告警用的 {@link NoticeService}，包含飞书和可选的邮件发送器。
     */
    @Bean
    public NoticeService noticeService(
            @Value("${migration.alert.email.username:}") String emailUsername,
            @Value("${migration.alert.email.password:}") String emailPassword,
            @Value("${migration.alert.email.host:}") String emailHost,
            @Value("${migration.alert.email.port:465}") int emailPort) {

        List<NoticeSender> senders = new ArrayList<>();
        senders.add(new FeishuNoticeSender());

        if (emailUsername != null && !emailUsername.isBlank()) {
            Properties props = new Properties();
            props.put("mail.smtp.host", emailHost);
            props.put("mail.smtp.port", String.valueOf(emailPort));
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.ssl.enable", "true");
            senders.add(new EmailNoticeSender(props, emailUsername, emailPassword));
            log.info("Email alert sender configured, host={}, port={}", emailHost, emailPort);
        }

        return new NoticeService(senders);
    }
}
