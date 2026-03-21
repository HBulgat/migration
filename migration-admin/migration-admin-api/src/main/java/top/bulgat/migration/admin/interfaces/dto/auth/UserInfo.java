package top.bulgat.migration.admin.interfaces.dto.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "用户信息")
public record UserInfo(
        @Schema(description = "用户名")
        @JsonProperty("username") String username,
        @Schema(description = "显示名称")
        @JsonProperty("display_name") String displayName
) {
}
