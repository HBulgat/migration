package top.bulgat.migration.admin.interfaces.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.List;

@Data
public class CreateAlertRuleRequest {
    @NotBlank(message = "migration_key cannot be blank")
    @JsonProperty("migration_key")
    private String migrationKey;

    @NotBlank(message = "name cannot be blank")
    @JsonProperty("name")
    private String name;

    @NotNull(message = "enable cannot be null")
    @JsonProperty("enable")
    private Boolean enable;

    @NotBlank(message = "channel cannot be blank")
    @JsonProperty("channel")
    private String channel;

    @NotBlank(message = "template_key cannot be blank")
    @JsonProperty("template_key")
    private String templateKey;

    @NotNull(message = "receivers cannot be null")
    @JsonProperty("receivers")
    private List<String> receivers;
}
