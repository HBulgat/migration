package top.bulgat.migration.diff.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import top.bulgat.common.notice.NoticeService;
import top.bulgat.common.notice.email.EmailNoticeSender;
import top.bulgat.common.notice.email.EmailSenderMeta;
import top.bulgat.common.notice.feishu.FeishuNoticeSender;

import java.util.List;

@Configuration
public class NoticeConfig {

    @Bean
    public FeishuNoticeSender feishuNoticeSender(ObjectMapper objectMapper){
        return new FeishuNoticeSender(new OkHttpClient(),objectMapper);
    }

    @Bean
    @ConfigurationProperties(prefix = "migration-diff.notice.email")
    public EmailSenderMeta emailSenderMeta(){
        return new EmailSenderMeta();
    }
    @Bean
    public EmailNoticeSender emailNoticeSender(EmailSenderMeta emailSenderMeta){
        return new EmailNoticeSender(emailSenderMeta);
    }

    @Bean
    public NoticeService noticeService(EmailNoticeSender emailNoticeSender,FeishuNoticeSender feishuNoticeSender){
        return new NoticeService(List.of(emailNoticeSender,feishuNoticeSender));
    }
}
