package top.bulgat.migration.admin.interfaces.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import top.bulgat.common.notice.NoticeChannel;

@Data
public class CreateAlertTemplateRequest {

    @NotBlank(message = "templateKey cannot be black")
    private String templateKey;

    @NotNull(message = "channel cannot be null")
    private NoticeChannel channel;

    @NotBlank(message = "name cannot be blank")
    private String name;

    @NotNull(message = "template cannot be null")
    private JsonNode template;
}
