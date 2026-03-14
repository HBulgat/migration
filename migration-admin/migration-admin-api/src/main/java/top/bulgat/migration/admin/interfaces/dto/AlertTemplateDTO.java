package top.bulgat.migration.admin.interfaces.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;
import lombok.Data;
import top.bulgat.common.notice.NoticeChannel;
import top.bulgat.migration.admin.domain.model.AlertTemplate;

import java.time.LocalDateTime;

@Data
@Builder
public class AlertTemplateDTO {

    private String templateKey;
    private NoticeChannel channel;
    private String name;
    private JsonNode template;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    public static AlertTemplateDTO from(AlertTemplate t) {
        return AlertTemplateDTO.builder()
                .templateKey(t.getTemplateKey())
                .channel(t.getChannel())
                .name(t.getName())
                .template(t.getTemplate())
                .createTime(t.getCreateTime())
                .updateTime(t.getUpdateTime())
                .build();
    }
}
