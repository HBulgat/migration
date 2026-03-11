package top.bulgat.migration.config.common.model.dataobject;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

public record AlertTemplateConfig(
        @JsonProperty("channel") String channel,
        @JsonProperty("name") String name,
        @JsonProperty("template") JsonNode template
) {
}
