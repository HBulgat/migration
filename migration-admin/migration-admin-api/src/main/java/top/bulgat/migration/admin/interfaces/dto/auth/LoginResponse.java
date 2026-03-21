package top.bulgat.migration.admin.interfaces.dto.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "登录响应")
public record LoginResponse(
        @Schema(description = "访问令牌")
        @JsonProperty("access_token") String accessToken,
        @Schema(description = "用户信息")
        @JsonProperty("user_info") UserInfo userInfo
) {
}
